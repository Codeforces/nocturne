package org.nocturne.ddos;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.nocturne.util.StringUtil;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class PowFilter implements Filter {
    private static final Logger logger = Logger.getLogger(PowFilter.class);

    private static final String X_REAL_IP = "X-Real-IP";

    private static final boolean logging = System.getProperty("PowFilter.logging", "false").equals("true");

    private static final Random RANDOM = new SecureRandom(Long.toString(System.nanoTime()
            ^ System.currentTimeMillis()
            ^ Runtime.getRuntime().freeMemory()).getBytes(StandardCharsets.UTF_8));

    private static final List<RequestFilter> REQUEST_FILTERS = new ArrayList<>();

    private static final ThreadLocal<String> rayIdLocal = new ThreadLocal<>();

    @Override
    public void init(FilterConfig filterConfig) {
        // No operations.
    }

    @Override
    public void destroy() {
        // No operations.
    }

    private void info(String message) {
        if (logging) {
            message = "PowFilter: powRayId=" + rayIdLocal.get() + ": " + message;
            logger.info(message);

            String print = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ": " + message;
            System.out.println(print);
            System.err.println(print);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;

            String rayId = RandomStringUtils.randomAlphanumeric(8);
            rayIdLocal.set(rayId);

            info("Starting processing request [uri=" + httpServletRequest.getRequestURI()
                    + ", url=" + httpServletRequest.getRequestURL()
                    + ", query=" + httpServletRequest.getQueryString()
                    + ", ip=" + getIp(httpServletRequest) + "].");

            if (logging) {
                info("Headers:");
                Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    info("    " + headerName + ": " + httpServletRequest.getHeader(headerName));
                }

                HttpSession session = httpServletRequest.getSession(true);
                if (session != null) {
                    info("Session attributes:");
                    Enumeration<String> attributeNames = session.getAttributeNames();
                    while (attributeNames.hasMoreElements()) {
                        String attributeName = attributeNames.nextElement();
                        info("    " + attributeName + ": " + session.getAttribute(attributeName));
                    }
                }

                info("Cookies:");
                Cookie[] cookies = httpServletRequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        info("    " + cookie.getName() + ": " + cookie.getValue());
                    }
                }
            }

            for (RequestFilter requestFilter : REQUEST_FILTERS) {
                Integer verdict = requestFilter.filter(httpServletRequest);
                info("Request filter " + requestFilter.getClass().getSimpleName() + " returned " + verdict + ".");

                if (verdict != null) {
                    if (verdict == 0) {
                        info("Do 'chain.doFilter(request, response);' and return.");
                        chain.doFilter(request, response);
                    } else {
                        info("Send error " + verdict + " and return.");
                        httpServletResponse.sendError(verdict);
                    }
                    return;
                }
            }

            doInternalFilter(httpServletRequest, httpServletResponse, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    private static String getIp(HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader(X_REAL_IP);
        if (StringUtil.isNotEmpty(ip)) {
            return ip;
        } else {
            return httpRequest.getRemoteAddr();
        }
    }

    private static String getUserAgent(HttpServletRequest httpRequest) {
        return httpRequest.getHeader("User-Agent");
    }

    private static String getRequestFingerprint(HttpServletRequest request) {
        return "#" + getIp(request) + "!" + getUserAgent(request);
    }

    private void doInternalFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = request.getSession();

        String secret = (String) session.getAttribute("secret");
        String sha = (String) session.getAttribute("sha");
        String requestFingerprint = getRequestFingerprint(request);

        info("sessionId= " + session.getId()
                + ", secret=" + secret
                + ", sha=" + sha
                + ", requestFingerprint=" + requestFingerprint + ".");

        if (StringUtil.isEmpty(secret)
                || StringUtil.isEmpty(sha)
                || !sha.equals(DigestUtils.sha1Hex(secret + requestFingerprint))) {
            secret = nextSecret();
            session.setAttribute("secret", secret);
            sha = DigestUtils.sha1Hex(secret + requestFingerprint);
            session.setAttribute("sha", sha);
            info("If empty case: secret=" + secret + ", sha=" + sha + ".");
        }

        String half = sha.substring(0, 20);
        String cookie = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (c.getName().equals("pow")) {
                    cookie = c.getValue();
                    break;
                }
            }
        }

        info("half=" + half + ", cookie=" + cookie + ".");

        if (cookie != null && cookie.equals(sha)) {
            info("cookie != null && cookie.equals(sha).");
            chain.doFilter(request, response);
        } else if (cookie != null && isResult(cookie, half)) {
            info("cookie != null && isResult(cookie, half): cookie=" + cookie + ", half=" + half + ".");
            Cookie powCookie = new Cookie("pow", sha);
            powCookie.setPath("/");
            powCookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));
            response.addCookie(powCookie);
            info("Set-Cookie: pow=" + sha + ".");
            chain.doFilter(request, response);
        } else {
            info("else case: cookie=" + cookie + ", half=" + half + ".");
            Cookie powCookie = new Cookie("pow", half);
            powCookie.setPath("/");
            powCookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));
            response.addCookie(powCookie);
            response.setContentType("text/html");
            printResponse(response);
            info("writer.flush(), Set-Cookie: pow=" + half + ".");
        }
    }

    private static void printResponse(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println("<style>\n" +
                "p {\n" +
                "    height: 100vh;\n" +
                "    display: flex;\n" +
                "    justify-content: center;\n" +
                "    align-items: center;\n" +
                "}\n" +
                "</style>\n<p>Please wait. Your browser is being checked. It may take a few seconds...</p>");
        writer.println("<script>");
        writer.println(getObfuscatedJsCode());
//        writer.println(getJsCode());
        writer.println("</script>");
        writer.flush();
    }

    private boolean isResult(String cookie, String halfSecret) {
        if (StringUtil.isNotEmpty(cookie) && cookie.endsWith("_" + halfSecret)) {
            String hash = DigestUtils.sha1Hex(cookie);
            return hash.startsWith("0000");
        } else {
            return false;
        }
    }

    private synchronized static String nextSecret() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            result.append(RANDOM.nextInt());
        }
        return DigestUtils.sha1Hex(result.toString());
    }

    private static String getObfuscatedJsCode() {
        return "var _0x3e09e3=_0x4bf8;(function(_0x42d959,_0x647252){var _0x25aba4=_0x4bf8,_0x437db0=_0x42d959();while(!![]){try{var _0x8e1627=-parseInt(_0x25aba4(0xa7))/0x1*(-parseInt(_0x25aba4(0x74))/0x2)+-parseInt(_0x25aba4(0xac))/0x3*(-parseInt(_0x25aba4(0x80))/0x4)+parseInt(_0x25aba4(0x98))/0x5*(parseInt(_0x25aba4(0xaa))/0x6)+parseInt(_0x25aba4(0x9d))/0x7+-parseInt(_0x25aba4(0x8a))/0x8*(-parseInt(_0x25aba4(0xab))/0x9)+parseInt(_0x25aba4(0x91))/0xa*(-parseInt(_0x25aba4(0x89))/0xb)+parseInt(_0x25aba4(0x72))/0xc*(-parseInt(_0x25aba4(0x7a))/0xd);if(_0x8e1627===_0x647252)break;else _0x437db0['push'](_0x437db0['shift']());}catch(_0x5d7d1d){_0x437db0['push'](_0x437db0['shift']());}}}(_0x2d66,0x4a27d));var _cs=['s=',_0x3e09e3(0xb6),_0x3e09e3(0x9a),'1024','0',_0x3e09e3(0xad),_0x3e09e3(0x94),';',_0x3e09e3(0x97),'=',_0x3e09e3(0x70),'\\x20','_',_0x3e09e3(0xae),'\\x0a',';p','000','=/','By',';','+','e','rCa','nav',_0x3e09e3(0xb5),'lo','bx','bst','bs','St',_0x3e09e3(0x9b),'it','arC','6c','th','ch',_0x3e09e3(0xa9),'mC','uk',_0x3e09e3(0xa6),'-',_0x3e09e3(0xb2),_0x3e09e3(0x96),'eAt','o7r','lz','omC','su','tr','c9','ri','eA','vi',_0x3e09e3(0x71),_0x3e09e3(0xb0),'h',_0x3e09e3(0x9c),'n6','gth','xl','jp','t','b64',_0x3e09e3(0x73),'b8y','ym','0','st','Of','hu','v4','co','gt',_0x3e09e3(0x8c),'4v','Ti','30','2n',_0x3e09e3(0x78),'pu','s9',_0x3e09e3(0x6f),'By','mCh','st','ki','zone','&','spl',_0x3e09e3(0x95),_0x3e09e3(0x76),'xnf','f7v','gbs','set',_0x3e09e3(0xa5),_0x3e09e3(0x7d),'re','h3q','zx','ad',_0x3e09e3(0x8d),'uhd',_0x3e09e3(0x86),'gt','ng',_0x3e09e3(0xb3),_0x3e09e3(0x81),_0x3e09e3(0x7b),'efm','ye','Ch',_0x3e09e3(0xa3),_0x3e09e3(0x9f),'hf','for','1wy',_0x3e09e3(0x88),'ow',_0x3e09e3(0xad),'Lo','to',_0x3e09e3(0xb1),'le','5r',_0x3e09e3(0xaf),'wph','get','7f','y3','Co',_0x3e09e3(0xa1),'sd','ce',_0x3e09e3(0xa4),_0x3e09e3(0x99),'fr','h7','t2','7y','dex','7c',_0x3e09e3(0x92),_0x3e09e3(0x83),'rC',_0x3e09e3(0xb6),_0x3e09e3(0xb4),'dow','od',_0x3e09e3(0x84),_0x3e09e3(0x7f),_0x3e09e3(0x79),'math','de',_0x3e09e3(0xa8),_0x3e09e3(0x8f),'At',_0x3e09e3(0x77),_0x3e09e3(0x7c),'in',_0x3e09e3(0x75),_0x3e09e3(0x82),_0x3e09e3(0x85),'har','fz',_0x3e09e3(0xa0),'Id','pa','9h','we','sh','me','se','m9','om','toU',_0x3e09e3(0x93),'rCo','g','w2',_0x3e09e3(0x7e),_0x3e09e3(0x8b),'hm','+','9v','0s',_0x3e09e3(0x9e),'h4',_0x3e09e3(0x8e),_0x3e09e3(0xa2),'%','t3z','5dk','bn',_0x3e09e3(0x99),'len',_0x3e09e3(0x87),'ar','ji',_0x3e09e3(0x90)];function _0x2d66(){var _0x9a6442=['nfl','n9s','pla','83v','fro','cha','60hKQkDX','abs','loc','ath','get','1024','ire','342455dubHwB','time','pow','mkk','nea','11382oqgMyh','Cha','7ox','qny','TCS','lau','win','geo','func','gs8','16181BJNMSb','Cod','sls','48axsGzc','95094eEcjTt','3CeVykH','Ele','exp','801','8jn','p66','tion','oki','sgg','0fi','pop','smp','nav','68m','60zvMLPs','rin','42qVGOUy','djt','ment','pus','ode','while','2725346qegrXi','sub','9ig','16u','tri','Str','392408ydMhjj','8a5','5i0','toS','pc2','nci','coo','l5u','irs','125972wOBRSc','328tXaqCx'];_0x2d66=function(){return _0x9a6442;};return _0x2d66();}function _f6(_0x2a5a76){function _0x2c4077(_0x4cb62a,_0x58288a){return _0x4cb62a<<_0x58288a|_0x4cb62a>>>0x20-_0x58288a;}function _0x22e96f(_0x1a925f){var _0x6b87c1='',_0x3b5310,_0x6446c7,_0x3a0707;for(_0x3b5310=0x0;_0x3b5310<=0x6;_0x3b5310+=0x2){_0x6446c7=_0x1a925f>>>_0x3b5310*0x4+0x4&0xf,_0x3a0707=_0x1a925f>>>_0x3b5310*0x4&0xf,_0x6b87c1+=_0x6446c7[_cs[0x8f]+_cs[0xb4]+_cs[0x69]](0x10)+_0x3a0707[_cs[0x79]+_cs[0x1d]+_cs[0x3f]+_cs[0xb2]](0x10);}return _0x6b87c1;}function _0x21b5bb(_0x5336ac){var _0x27054a='',_0x23d630,_0xc498df;for(_0x23d630=0x7;_0x23d630>=0x0;_0x23d630--){_0xc498df=_0x5336ac>>>_0x23d630*0x4&0xf,_0x27054a+=_0xc498df[_cs[0x8f]+_cs[0x30]+_cs[0x9f]+_cs[0xb2]](0x10);}return _0x27054a;}function _0x1d3ea3(_0x29925b){_0x29925b=_0x29925b[_cs[0x61]+_cs[0x65]+_cs[0x85]](/\\r\\_vs/g,_cs[0xe]);var _0x429bd5='';for(var _0x2911f=0x0;_0x2911f<_0x29925b[_cs[0x7b]+_cs[0x69]+_cs[0x22]];_0x2911f++){var _0x25f897=_0x29925b[_cs[0x23]+_cs[0x20]+_cs[0x4e]+_cs[0x9c]](_0x2911f);if(_0x25f897<0x80)_0x429bd5+=String[_cs[0x88]+_cs[0x2e]+_cs[0xa3]+_cs[0x9a]+_cs[0x15]](_0x25f897);else _0x25f897>0x7f&&_0x25f897<0x800?(_0x429bd5+=String[_cs[0x9b]+_cs[0x25]+_cs[0xa3]+_cs[0x9a]+_cs[0x15]](_0x25f897>>0x6|0xc0),_0x429bd5+=String[_cs[0x88]+_cs[0xae]+_cs[0xba]+_cs[0x90]+_cs[0x4e]](_0x25f897&0x3f|0x80)):(_0x429bd5+=String[_cs[0x9b]+_cs[0x53]+_cs[0xc5]+_cs[0x9a]+_cs[0x15]](_0x25f897>>0xc|0xe0),_0x429bd5+=String[_cs[0x88]+_cs[0xae]+_cs[0xba]+_cs[0x90]+_cs[0x4e]](_0x25f897>>0x6&0x3f|0x80),_0x429bd5+=String[_cs[0x88]+_cs[0xae]+_cs[0x6f]+_cs[0xc5]+_cs[0x82]+_cs[0x99]](_0x25f897&0x3f|0x80));}return _0x429bd5;}var _0x12612f,_0x12ad81,_0x1fd6c0,_0x1236c6=new Array(0x50),_0x199b1a=0x67452301,_0x475b8a=0xefcdab89,_0x45b004=0x98badcfe,_0x52f4a0=0x10325476,_0x871e14=0xc3d2e1f0,_0x183503,_0x76a6cd,_0x3e39cc,_0x19c81d,_0x50622b,_0x365471;_0x2a5a76=_0x1d3ea3(_0x2a5a76);var _0x46ffd3=_0x2a5a76[_cs[0x7b]+_cs[0x69]+_cs[0x22]],_0x13016d=new Array();for(_0x12ad81=0x0;_0x12ad81<_0x46ffd3-0x3;_0x12ad81+=0x4){_0x1fd6c0=_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x9a]+_cs[0x33]+_cs[0x3d]](_0x12ad81)<<0x18|_0x2a5a76[_cs[0xc7]+_cs[0x90]+_cs[0x94]+_cs[0x2b]](_0x12ad81+0x1)<<0x10|_0x2a5a76[_cs[0xc7]+_cs[0xb1]+_cs[0x99]+_cs[0x9c]](_0x12ad81+0x2)<<0x8|_0x2a5a76[_cs[0xc7]+_cs[0x90]+_cs[0x4e]+_cs[0x9c]](_0x12ad81+0x3),_0x13016d[_cs[0x4f]+_cs[0xaa]](_0x1fd6c0);}switch(_0x46ffd3%0x4){case 0x0:_0x12ad81=0x80000000;break;case 0x1:_0x12ad81=_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x9a]+_cs[0x2b]](_0x46ffd3-0x1)<<0x18|0x800000;break;case 0x2:_0x12ad81=_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x82]+_cs[0x99]+_cs[0x9c]](_0x46ffd3-0x2)<<0x18|_0x2a5a76[_cs[0xc7]+_cs[0x90]+_cs[0x4e]+_cs[0x9c]](_0x46ffd3-0x1)<<0x10|0x8000;break;case 0x3:_0x12ad81=_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x82]+_cs[0x99]+_cs[0x9c]](_0x46ffd3-0x3)<<0x18|_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x9a]+_cs[0x2b]](_0x46ffd3-0x2)<<0x10|_0x2a5a76[_cs[0x23]+_cs[0xc5]+_cs[0x9a]+_cs[0x2b]](_0x46ffd3-0x1)<<0x8|0x80;break;}_0x13016d[_cs[0x4f]+_cs[0xaa]](_0x12ad81);while(_0x13016d[_cs[0x7b]+_cs[0x69]+_cs[0x22]]%0x10!=0xe)_0x13016d[_cs[0x9d]+_cs[0x37]](0x0);_0x13016d[_cs[0x9d]+_cs[0x37]](_0x46ffd3>>>0x1d),_0x13016d[_cs[0x4f]+_cs[0xaa]](_0x46ffd3<<0x3&0xffffffff);for(_0x12612f=0x0;_0x12612f<_0x13016d[_cs[0xc3]+_cs[0x48]+_cs[0x37]];_0x12612f+=0x10){for(_0x12ad81=0x0;_0x12ad81<0x10;_0x12ad81++)_0x1236c6[_0x12ad81]=_0x13016d[_0x12612f+_0x12ad81];for(_0x12ad81=0x10;_0x12ad81<=0x4f;_0x12ad81++)_0x1236c6[_0x12ad81]=_0x2c4077(_0x1236c6[_0x12ad81-0x3]^_0x1236c6[_0x12ad81-0x8]^_0x1236c6[_0x12ad81-0xe]^_0x1236c6[_0x12ad81-0x10],0x1);_0x183503=_0x199b1a,_0x76a6cd=_0x475b8a,_0x3e39cc=_0x45b004,_0x19c81d=_0x52f4a0,_0x50622b=_0x871e14;for(_0x12ad81=0x0;_0x12ad81<=0x13;_0x12ad81++){_0x365471=_0x2c4077(_0x183503,0x5)+(_0x76a6cd&_0x3e39cc|~_0x76a6cd&_0x19c81d)+_0x50622b+_0x1236c6[_0x12ad81]+0x5a827999&0xffffffff,_0x50622b=_0x19c81d,_0x19c81d=_0x3e39cc,_0x3e39cc=_0x2c4077(_0x76a6cd,0x1e),_0x76a6cd=_0x183503,_0x183503=_0x365471;}for(_0x12ad81=0x14;_0x12ad81<=0x27;_0x12ad81++){_0x365471=_0x2c4077(_0x183503,0x5)+(_0x76a6cd^_0x3e39cc^_0x19c81d)+_0x50622b+_0x1236c6[_0x12ad81]+0x6ed9eba1&0xffffffff,_0x50622b=_0x19c81d,_0x19c81d=_0x3e39cc,_0x3e39cc=_0x2c4077(_0x76a6cd,0x1e),_0x76a6cd=_0x183503,_0x183503=_0x365471;}for(_0x12ad81=0x28;_0x12ad81<=0x3b;_0x12ad81++){_0x365471=_0x2c4077(_0x183503,0x5)+(_0x76a6cd&_0x3e39cc|_0x76a6cd&_0x19c81d|_0x3e39cc&_0x19c81d)+_0x50622b+_0x1236c6[_0x12ad81]+0x8f1bbcdc&0xffffffff,_0x50622b=_0x19c81d,_0x19c81d=_0x3e39cc,_0x3e39cc=_0x2c4077(_0x76a6cd,0x1e),_0x76a6cd=_0x183503,_0x183503=_0x365471;}for(_0x12ad81=0x3c;_0x12ad81<=0x4f;_0x12ad81++){_0x365471=_0x2c4077(_0x183503,0x5)+(_0x76a6cd^_0x3e39cc^_0x19c81d)+_0x50622b+_0x1236c6[_0x12ad81]+0xca62c1d6&0xffffffff,_0x50622b=_0x19c81d,_0x19c81d=_0x3e39cc,_0x3e39cc=_0x2c4077(_0x76a6cd,0x1e),_0x76a6cd=_0x183503,_0x183503=_0x365471;}_0x199b1a=_0x199b1a+_0x183503&0xffffffff,_0x475b8a=_0x475b8a+_0x76a6cd&0xffffffff,_0x45b004=_0x45b004+_0x3e39cc&0xffffffff,_0x52f4a0=_0x52f4a0+_0x19c81d&0xffffffff,_0x871e14=_0x871e14+_0x50622b&0xffffffff;}var _0x365471=_0x21b5bb(_0x199b1a)+_0x21b5bb(_0x475b8a)+_0x21b5bb(_0x45b004)+_0x21b5bb(_0x52f4a0)+_0x21b5bb(_0x871e14);return _0x365471[_cs[0x79]+_cs[0x78]+_cs[0xa9]+_cs[0x16]+_cs[0xac]]();}function _f1(_0x115978){let _0x4b525c=_0x115978+_cs[0x9],_0x31b88d=decodeURIComponent(document[_cs[0x67]+_cs[0x55]+_cs[0x15]]),_0xcde38c=_0x31b88d[_cs[0x58]+_cs[0x1f]](_cs[0x13]);for(let _0x39e78f=0x0;_0x39e78f<_0xcde38c[_cs[0x7b]+_cs[0x69]+_cs[0x22]];_0x39e78f++){let _0xde7646=_0xcde38c[_0x39e78f];while(_0xde7646[_cs[0x23]+_cs[0xc5]+_cs[0x9c]](0x0)==_cs[0xb]){_0xde7646=_0xde7646[_cs[0x2f]+_cs[0x1c]+_cs[0x30]+_cs[0x9f]+_cs[0xb2]](0x1);}if(_0xde7646[_cs[0x9f]+_cs[0x8c]+_cs[0x44]](_0x4b525c)==0x0)return _0xde7646[_cs[0x2f]+_cs[0x1b]+_cs[0x32]+_cs[0x69]](_0x4b525c[_cs[0xc3]+_cs[0x3a]],_0xde7646[_cs[0xc3]+_cs[0x48]+_cs[0x37]]);}return'';}function _f0(_0x50f2e7,_0x4ba3c7,_0x12bed1){const _0x27a246=new Date();_0x27a246[_cs[0x5e]+_cs[0x4b]+_cs[0xab]](_0x27a246[_cs[0x59]+_cs[0x4b]+_cs[0xab]]()+_0x12bed1*0x18*0x3c*0x3c*0x3e8);let _0x30919e=_cs[0xd]+_cs[0x8]+_cs[0x0]+_0x27a246[_cs[0xaf]+_cs[0x83]+_cs[0xb4]+_cs[0x69]]();document[_cs[0x47]+_cs[0x6a]+_cs[0x15]]=_0x50f2e7+_cs[0x9]+_0x4ba3c7+_cs[0x7]+_0x30919e+_cs[0xf]+_cs[0x6]+_cs[0x11];}function _0x4bf8(_0x146005,_0x391096){var _0x2d6660=_0x2d66();return _0x4bf8=function(_0x4bf803,_0x159e44){_0x4bf803=_0x4bf803-0x6f;var _0x3d171d=_0x2d6660[_0x4bf803];return _0x3d171d;},_0x4bf8(_0x146005,_0x391096);}setTimeout(function(){var _0x2de4ef=_f1(_cs[0x2])[_cs[0x6c]+_cs[0x43]+_cs[0x3f]+_cs[0xb2]](0x0,0x14),_0x1758c1=0x0;for(_0x1758c1=0x0;;_0x1758c1++){var _0x1bbbb5=_0x1758c1[_cs[0x79]+_cs[0x96]+_cs[0x9f]+_cs[0xb2]]()+_cs[0xc]+_0x2de4ef,_0x2f8ab6=_f6(_0x1bbbb5),_0x2a4818=_0x2f8ab6[_cs[0x6c]+_cs[0x43]+_cs[0x32]+_cs[0x69]](0x0,0x4);if(_0x2a4818===_cs[0x10]+_cs[0x4]){_f0(_cs[0x2],_0x1bbbb5,0x1),location[_cs[0x61]+_cs[0x19]+_cs[0x64]]();break;}}},0x64);";
    }

    @SuppressWarnings("unused")
    private static String getJsCode() {
        return getSha1Code() + getAndSetCookieCode() + getPowCode();
    }

    private static String getSha1Code() {
        return "function sha1(msg) {\n" +
                "    function rotate_left(n, s) {\n" +
                "        return (n << s) | (n >>> (32 - s));\n" +
                "    }\n" +
                "\n" +
                "    function lsb_hex(val) {\n" +
                "        var str = '';\n" +
                "        var i;\n" +
                "        var vh;\n" +
                "        var vl;\n" +
                "        for (i = 0; i <= 6; i += 2) {\n" +
                "            vh = (val >>> (i * 4 + 4)) & 0x0f;\n" +
                "            vl = (val >>> (i * 4)) & 0x0f;\n" +
                "            str += vh.toString(16) + vl.toString(16);\n" +
                "        }\n" +
                "        return str;\n" +
                "    }\n" +
                "\n" +
                "    function cvt_hex(val) {\n" +
                "        var str = '';\n" +
                "        var i;\n" +
                "        var v;\n" +
                "        for (i = 7; i >= 0; i--) {\n" +
                "            v = (val >>> (i * 4)) & 0x0f;\n" +
                "            str += v.toString(16);\n" +
                "        }\n" +
                "        return str;\n" +
                "    }\n" +
                "\n" +
                "    function Utf8Encode(string) {\n" +
                "        string = string.replace(/\\r\\n/g, '\\n');\n" +
                "        var utftext = '';\n" +
                "        for (var n = 0; n < string.length; n++) {\n" +
                "            var c = string.charCodeAt(n);\n" +
                "            if (c < 128) {\n" +
                "                utftext += String.fromCharCode(c);\n" +
                "            } else if ((c > 127) && (c < 2048)) {\n" +
                "                utftext += String.fromCharCode((c >> 6) | 192);\n" +
                "                utftext += String.fromCharCode((c & 63) | 128);\n" +
                "            } else {\n" +
                "                utftext += String.fromCharCode((c >> 12) | 224);\n" +
                "                utftext += String.fromCharCode(((c >> 6) & 63) | 128);\n" +
                "                utftext += String.fromCharCode((c & 63) | 128);\n" +
                "            }\n" +
                "        }\n" +
                "        return utftext;\n" +
                "    }\n" +
                "\n" +
                "    var blockstart;\n" +
                "    var i, j;\n" +
                "    var W = new Array(80);\n" +
                "    var H0 = 0x67452301;\n" +
                "    var H1 = 0xEFCDAB89;\n" +
                "    var H2 = 0x98BADCFE;\n" +
                "    var H3 = 0x10325476;\n" +
                "    var H4 = 0xC3D2E1F0;\n" +
                "    var A, B, C, D, E;\n" +
                "    var temp;\n" +
                "    msg = Utf8Encode(msg);\n" +
                "    var msg_len = msg.length;\n" +
                "    var word_array = new Array();\n" +
                "    for (i = 0; i < msg_len - 3; i += 4) {\n" +
                "        j = msg.charCodeAt(i) << 24 | msg.charCodeAt(i + 1) << 16 |\n" +
                "            msg.charCodeAt(i + 2) << 8 | msg.charCodeAt(i + 3);\n" +
                "        word_array.push(j);\n" +
                "    }\n" +
                "    switch (msg_len % 4) {\n" +
                "        case 0:\n" +
                "            i = 0x080000000;\n" +
                "            break;\n" +
                "        case 1:\n" +
                "            i = msg.charCodeAt(msg_len - 1) << 24 | 0x0800000;\n" +
                "            break;\n" +
                "        case 2:\n" +
                "            i = msg.charCodeAt(msg_len - 2) << 24 | msg.charCodeAt(msg_len - 1) << 16 | 0x08000;\n" +
                "            break;\n" +
                "        case 3:\n" +
                "            i = msg.charCodeAt(msg_len - 3) << 24 | msg.charCodeAt(msg_len - 2) << 16 | msg.charCodeAt(msg_len - 1) << 8 | 0x80;\n" +
                "            break;\n" +
                "    }\n" +
                "    word_array.push(i);\n" +
                "    while ((word_array.length % 16) != 14) word_array.push(0);\n" +
                "    word_array.push(msg_len >>> 29);\n" +
                "    word_array.push((msg_len << 3) & 0x0ffffffff);\n" +
                "    for (blockstart = 0; blockstart < word_array.length; blockstart += 16) {\n" +
                "        for (i = 0; i < 16; i++) W[i] = word_array[blockstart + i];\n" +
                "        for (i = 16; i <= 79; i++) W[i] = rotate_left(W[i - 3] ^ W[i - 8] ^ W[i - 14] ^ W[i - 16], 1);\n" +
                "        A = H0;\n" +
                "        B = H1;\n" +
                "        C = H2;\n" +
                "        D = H3;\n" +
                "        E = H4;\n" +
                "        for (i = 0; i <= 19; i++) {\n" +
                "            temp = (rotate_left(A, 5) + ((B & C) | (~B & D)) + E + W[i] + 0x5A827999) & 0x0ffffffff;\n" +
                "            E = D;\n" +
                "            D = C;\n" +
                "            C = rotate_left(B, 30);\n" +
                "            B = A;\n" +
                "            A = temp;\n" +
                "        }\n" +
                "        for (i = 20; i <= 39; i++) {\n" +
                "            temp = (rotate_left(A, 5) + (B ^ C ^ D) + E + W[i] + 0x6ED9EBA1) & 0x0ffffffff;\n" +
                "            E = D;\n" +
                "            D = C;\n" +
                "            C = rotate_left(B, 30);\n" +
                "            B = A;\n" +
                "            A = temp;\n" +
                "        }\n" +
                "        for (i = 40; i <= 59; i++) {\n" +
                "            temp = (rotate_left(A, 5) + ((B & C) | (B & D) | (C & D)) + E + W[i] + 0x8F1BBCDC) & 0x0ffffffff;\n" +
                "            E = D;\n" +
                "            D = C;\n" +
                "            C = rotate_left(B, 30);\n" +
                "            B = A;\n" +
                "            A = temp;\n" +
                "        }\n" +
                "        for (i = 60; i <= 79; i++) {\n" +
                "            temp = (rotate_left(A, 5) + (B ^ C ^ D) + E + W[i] + 0xCA62C1D6) & 0x0ffffffff;\n" +
                "            E = D;\n" +
                "            D = C;\n" +
                "            C = rotate_left(B, 30);\n" +
                "            B = A;\n" +
                "            A = temp;\n" +
                "        }\n" +
                "        H0 = (H0 + A) & 0x0ffffffff;\n" +
                "        H1 = (H1 + B) & 0x0ffffffff;\n" +
                "        H2 = (H2 + C) & 0x0ffffffff;\n" +
                "        H3 = (H3 + D) & 0x0ffffffff;\n" +
                "        H4 = (H4 + E) & 0x0ffffffff;\n" +
                "    }\n" +
                "    var temp = cvt_hex(H0) + cvt_hex(H1) + cvt_hex(H2) + cvt_hex(H3) + cvt_hex(H4);\n" +
                "\n" +
                "    return temp.toLowerCase();\n" +
                "}\n\n";
    }

    private static String getAndSetCookieCode() {
        return "function getCookie(cname) {\n" +
                "  let name = cname + \"=\";\n" +
                "  let decodedCookie = decodeURIComponent(document.cookie);\n" +
                "  let ca = decodedCookie.split(';');\n" +
                "  for(let i = 0; i <ca.length; i++) {\n" +
                "    let c = ca[i];\n" +
                "    while (c.charAt(0) == ' ') {\n" +
                "      c = c.substring(1);\n" +
                "    }\n" +
                "    if (c.indexOf(name) == 0) {\n" +
                "      return c.substring(name.length, c.length);\n" +
                "    }\n" +
                "  }\n" +
                "  return \"\";\n" +
                "}\n\nfunction setCookie(cname, cvalue, exdays) {\n" +
                "  const d = new Date();\n" +
                "  d.setTime(d.getTime() + (exdays*24*60*60*1000));\n" +
                "  let expires = \"expires=\"+ d.toUTCString();\n" +
                "  document.cookie = cname + \"=\" + cvalue + \";\" + expires + \";path=/\";\n" +
                "}\n\n";
    }

    private static String getPowCode() {
        return "setTimeout(function() {\n" +
                "    var c = getCookie('pow').substring(0, 20);\n" +
                "    var i = 0;\n" +
                "    for (i = 0;; i++) {\n" +
                "        var s = i.toString() + '_' + c;\n" +
                "        var hash = sha1(s);\n" +
                "        var prefix = hash.substring(0, 4);\n" +
                "        if (prefix === \"0000\") {\n" +
                "            setCookie('pow', s, 1);\n" +
                "            location.reload();\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "}, 100);\n\n";
    }

    public static void addRequestFilter(RequestFilter filter) {
        REQUEST_FILTERS.add(filter);
    }

    public interface RequestFilter {
        /**
         * @param request {@link HttpServletRequest}
         * @return 0: pass, null: pass to next filter, other: response code
         */
        Integer filter(HttpServletRequest request);
    }
}
