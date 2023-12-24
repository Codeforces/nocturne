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
        return "var _cs5=['func','zone','Br','to','fr','86','WB','ing','tb','ti','b64','7ao','rC','tp','f','Co','4ys',\"while\",'rCa','ng','6z','v6','412','ne','yjG','%','in','bzj','4V','70r','=/','dow','0dc','tT','get','pop','wnw','okv','3gb','owe','71','xO','if','KS','zo','Ele','pY','mat','-','0','for',\"nav\",'mC','js','Ti','nav','cDf','uja','geo','nm','=','8K','ox6','gi','oas','lq','n','har',\"%\",'Id','it','28','gt','ft','9mw','win','lo','33','3s','ar','\\x0','0','wD','se','sh',\"pop\",'arC','Y','ha','7e',';','wpe','7v0','VU','7c','len','pj','3g7','tio','stq',\"get\",';p','DeW','xF','Id','whi','ngt','sbi','fun','ir','0hp','961','8w','ns','re','UTC','s',\"win\",'ie','th',\"Ele\",'Uo','1z',\"tion\",\"-\",'ok','36','cqu','bs','w6',\"loc\",'NLU','bst','uw','abs','_','ra','pu','At','UH','20','9n9','8ga','im','2ts','nt','10','1u','rO','00',\"b64\",'s5',\"time\",'z','fro','x4','+','set','421','pus','\\x','+','g','oki','w5','73','ment','qdw','for','22v','666','102','rq','257','c','spl','tr','FO','wh','pl','6n','dow','id','pow','wg','124','i4n','St','840','&','1c','kbk','shi','toS','vTt','ode','loc','e','zv','h','fb','ge','kim','ymf',\"geo\",'mCh','853','18','ad','omC','abs','31','rCo','9iG','Nq','ta','4','exp','7b1','5z5','0f','By','s8','1024','ch','gth','5Kx','math','By','yc','gju','cha','9i5','zki','rin','a32','XE','le','deA','WNm','tri','de','su','a7b','ile','co','wjr','5cg','CWB','8k','54','toL','ath','t','27','a','ovs','es=','ace','29','me','d2r','3Hn','10q']; function _f8(){var _v1j=[_v1k[106],_v1k[67],_v1k[203],_v1k[7],_v1k[75],_v1k[263],_v1k[170]+_v1k[188]+_v1k[220]+_v1k[24]+_v1k[6],_v1k[169],_v1k[213]+_v1k[82]+_v1k[139],_v1k[97],_v1k[193],_v1k[141],_v1k[132],_v1k[207]+_v1k[177]+_v1k[93]+_v1k[113],_v1k[205],_v1k[261],_v1k[183],_v1k[92],_v1k[36],_v1k[219],_v1k[247],_v1k[207]+_v1k[46]+_v1k[2]+_v1k[76],_v1k[99],_v1k[209],_v1k[251],_v1k[212],_v1k[18],_v1k[37],_v1k[168],_v1k[243],_v1k[240],_v1k[57],_v1k[246],_v1k[95],_v1k[257],_v1k[62],_v1k[225],_v1k[254]+_v1k[22]+_v1k[28]+_v1k[236]+_v1k[194],_v1k[91],_v1k[38],_v1k[71]+_v1k[148]+_v1k[56]+_v1k[116],_v1k[191],_v1k[260]+_v1k[145],_v1k[142],_v1k[163],_v1k[32],_v1k[171]+_v1k[216],_v1k[238],_v1k[230],_v1k[173]+_v1k[146]+_v1k[185]+_v1k[102]+_v1k[43]+_v1k[153],_v1k[111]+_v1k[5]+_v1k[226]+_v1k[103]+_v1k[214],_v1k[232],_v1k[159],_v1k[167],_v1k[9]+_v1k[260],_v1k[134],_v1k[235],_v1k[259]+_v1k[207]+_v1k[40]+_v1k[262]+_v1k[239]+_v1k[87],_v1k[31],_v1k[44]+_v1k[23],_v1k[178]+_v1k[244],_v1k[211]+_v1k[206]+_v1k[190]+_v1k[131]+_v1k[52],_v1k[10],_v1k[108]+_v1k[174],_v1k[11],_v1k[233],_v1k[34],_v1k[74],_v1k[58],_v1k[258],_v1k[157],_v1k[35],_v1k[127],_v1k[115],_v1k[196],_v1k[27],_v1k[158]+_v1k[126]+_v1k[61]+_v1k[248]+_v1k[121],_v1k[86],_v1k[39]];_g0=function(){return _v1j;};return _f8();}var _g1=_f2;(function(_0x42a5c6,_0x585de1){var _g2=_f2,_g3=_0x42a5c6();while(!![]){try{var _g4=parseInt(_g2(0xe1))/0x1+-parseInt(_g2(0x118))/0x2*(-parseInt(_g2(0x100))/0x3)+parseInt(_g2(0x11b))/0x4*(parseInt(_g2(0xd6))/0x5)+-parseInt(_g2(0x108))/0x6*(-parseInt(_g2(0xdd))/0x7)+parseInt(_g2(0xf0))/0x8*(-parseInt(_g2(0xfb))/0x9)+-parseInt(_g2(0xf9))/0xa+-parseInt(_g2(0xd5))/0xb;if(_g4===_0x585de1)break;else _g3[_cs5[137]+_cs5[84]](_g3[_cs5[192]+_cs5[73]]());}catch(_0x3c0da1){_g3[_cs5[137]+_cs5[84]](_g3[_cs5[84]+_cs5[42]+_cs5[253]]());}}}(_f8,0xa667c));var _g5=[_cs5[135],_g1(0xe2),_cs5[156],_cs5[90],_cs5[60],_g1(0xeb),_cs5[149],_cs5[90],_cs5[80]+_cs5[255],_g1(0x115),_cs5[217],_cs5[49],_cs5[101],_cs5[30],_cs5[160]+_cs5[140],_g1(0x103),_g1(0xda),_cs5[109],_cs5[252],_g1(0xee),_cs5[105]+_cs5[237],_cs5[107],_cs5[52],_g1(0x11f),_g1(0xee),_g1(0xfa),_g1(0xd7),_g1(0xd8),_cs5[179],_g1(0xed),_g1(0x10b),_cs5[250],_cs5[12],_g1(0x10e),_cs5[242],_g1(0x101),_cs5[98]+_cs5[66],_g1(0xde),_g1(0xe5),_g1(0x11d),_g1(0xdb),_g1(0x121),_cs5[110],_cs5[78],_cs5[187],_g1(0xe9),_g1(0x11e),_cs5[165],_cs5[122],_cs5[94],_cs5[3],_g1(0x10c),_g1(0xf2),_cs5[14],_cs5[147],_g1(0xff),_cs5[176],_g1(0x10a),_g1(0x114),_cs5[69],_cs5[202],_g1(0xf4),_cs5[143],_g1(0xea),_cs5[234],_g1(0xe3),_cs5[208],_cs5[133],_cs5[249],_g1(0x10f),_g1(0xe7),_cs5[180],_cs5[45],_g1(0x105),_cs5[165],_g1(0xef),_cs5[222],_g1(0x106),_cs5[64],_cs5[96],_cs5[215],_g1(0xf8),_cs5[253],_cs5[164],_cs5[25],_cs5[125],_cs5[197],_cs5[26],_cs5[49],_cs5[72],_g1(0xda),_cs5[143],_cs5[114],_cs5[55],_cs5[128],_g1(0x116),_cs5[138],_g1(0xdc),_g1(0x122),_g1(0x111),_cs5[48],_g1(0x113),_g1(0x117),_cs5[136],_cs5[84],_g1(0x119),_cs5[54],_g1(0x107),_cs5[76],_g1(0x102),_g1(0xfc),_cs5[256],_cs5[237],_cs5[112],_cs5[155],_g1(0xda),_cs5[199],_g1(0x123),_cs5[118],_g1(0xf5),_cs5[21],_g1(0xf7),_g1(0xf3),_cs5[189],_g1(0xf6),_cs5[83],_g1(0x120),_g1(0xfe),_cs5[186],_cs5[229],_cs5[137],_cs5[175],_cs5[129],_g1(0xe6),_cs5[33],_cs5[218],_cs5[89],_cs5[77],_cs5[88],_cs5[249],_cs5[20],_g1(0xe0),_cs5[19],_cs5[200],_cs5[8],_cs5[221],_cs5[63],_g1(0xe8),_cs5[224],_cs5[70],_g1(0x109),_g1(0xec),_cs5[151],_cs5[245],_g1(0xeb),_g1(0x11c),_cs5[241],_g1(0xe2),_cs5[4],_cs5[59],_cs5[231],_cs5[154],_g1(0xd9),_cs5[144],_cs5[41],_cs5[53],_g1(0x110),_cs5[195],_cs5[182],_cs5[15],_g1(0xe4),_cs5[260],_g1(0x112),_cs5[119],_cs5[201],_cs5[47]+_cs5[199],_g1(0x11a),_g1(0xf1),_g1(0xfd),_cs5[149],_cs5[184],_cs5[65],_cs5[79],_cs5[198],_g1(0x10d),_cs5[13],_cs5[29],_g1(0xdf),_cs5[156],_cs5[16],_cs5[172],_g1(0x104),_cs5[162]];function _f7(_pq){function _f7(_pp,_po){return _pp<<_po|_pp>>>0x20-_po;}function _f5(_pj){var _vr='',_vs,_vt,_vu;for(_vs=0x0;_vs<=0x6;_vs+=0x2){_vt=_pj>>>_vs*0x4+0x4&0xf,_vu=_pj>>>_vs*0x4&0xf,_vr+=_vt[_g5[0xb2]+_g5[0x63]+_g5[0x8e]](0x10)+_vu[_g5[0xb2]+_g5[0x63]+_g5[0x8e]](0x10);}return _vr;}function _f4(_pf){var _vv='',_vw,_vx;for(_vw=0x7;_vw>=0x0;_vw--){_vx=_pf>>>_vw*0x4&0xf,_vv+=_vx[_g5[0xb2]+_g5[0x63]+_g5[0x8e]](0x10);}return _vv;}function _0x198ea5(_pb){_pb=_pb[_g5[0x5c]+_g5[0x1c]+_g5[0x2d]](/\\r\\_vs/g,_g5[0x8]);var _vy='';for(var _vz=0x0;_vz<_pb[_g5[0x3a]+_g5[0x66]];_vz++){var _v10=_pb[_g5[0xa0]+_g5[0x33]+_g5[0x9c]+_g5[0x60]](_vz);if(_v10<0x80)_vy+=String[_g5[0xa1]+_g5[0x16]+_g5[0x8a]+_g5[0x33]+_g5[0x9c]](_v10);else _v10>0x7f&&_v10<0x800?(_vy+=String[_g5[0xa1]+_g5[0x23]+_g5[0xb6]+_g5[0xa9]+_g5[0x9c]](_v10>>0x6|0xc0),_vy+=String[_g5[0xa1]+_g5[0x16]+_g5[0x8a]+_g5[0x20]+_g5[0xa7]](_v10&0x3f|0x80)):(_vy+=String[_g5[0x9e]+_g5[0x39]+_g5[0x3d]+_g5[0xa9]+_g5[0x9c]](_v10>>0xc|0xe0),_vy+=String[_g5[0x9e]+_g5[0x39]+_g5[0x8a]+_g5[0x33]+_g5[0x9c]](_v10>>0x6&0x3f|0x80),_vy+=String[_g5[0xa1]+_g5[0x23]+_g5[0xb1]+_g5[0xa7]](_v10&0x3f|0x80));}return _vy;}var _v11,_v12,_v13,_v14=new Array(0x50),_v15=0x67452301,_v16=0xefcdab89,_v17=0x98badcfe,_v18=0x10325476,_v19=0xc3d2e1f0,_v1a,_v1b,_v1c,_v1d,_v1e,_v1f;_pq=_0x198ea5(_pq);var _v1g=_pq[_g5[0x3a]+_g5[0x66]],_v1h=new Array();for(_v12=0x0;_v12<_v1g-0x3;_v12+=0x4){_v13=_pq[_g5[0xa0]+_g5[0x33]+_g5[0x62]+_g5[0x52]](_v12)<<0x18|_pq[_g5[0xa0]+_g5[0x33]+_g5[0x62]+_g5[0x52]](_v12+0x1)<<0x10|_pq[_g5[0x94]+_g5[0xb6]+_g5[0xa9]+_g5[0x9c]+_g5[0x60]](_v12+0x2)<<0x8|_pq[_g5[0xa0]+_g5[0x33]+_g5[0x9c]+_g5[0x60]](_v12+0x3),_v1h[_g5[0x82]+_g5[0x68]](_v13);}switch(_v1g%0x4){case 0x0:_v12=0x80000000;break;case 0x1:_v12=_pq[_g5[0x94]+_g5[0xb6]+_g5[0xa9]+_g5[0x62]+_g5[0x52]](_v1g-0x1)<<0x18|0x800000;break;case 0x2:_v12=_pq[_g5[0xa0]+_g5[0x33]+_g5[0x9c]+_g5[0x60]](_v1g-0x2)<<0x18|_pq[_g5[0x94]+_g5[0xb6]+_g5[0xa9]+_g5[0x62]+_g5[0x52]](_v1g-0x1)<<0x10|0x8000;break;case 0x3:_v12=_pq[_g5[0xa0]+_g5[0x33]+_g5[0x9c]+_g5[0x60]](_v1g-0x3)<<0x18|_pq[_g5[0xa0]+_g5[0x20]+_g5[0xa7]+_g5[0x60]](_v1g-0x2)<<0x10|_pq[_g5[0xa0]+_g5[0x33]+_g5[0x9c]+_g5[0x60]](_v1g-0x1)<<0x8|0x80;break;}_v1h[_g5[0x82]+_g5[0x68]](_v12);while(_v1h[_g5[0x3a]+_g5[0x66]]%0x10!=0xe)_v1h[_g5[0x82]+_g5[0x68]](0x0);_v1h[_g5[0x1b]+_g5[0x74]](_v1g>>>0x1d),_v1h[_g5[0x1b]+_g5[0x74]](_v1g<<0x3&0xffffffff);for(_v11=0x0;_v11<_v1h[_g5[0x70]+_g5[0x7a]+_g5[0x74]];_v11+=0x10){for(_v12=0x0;_v12<0x10;_v12++)_v14[_v12]=_v1h[_v11+_v12];for(_v12=0x10;_v12<=0x4f;_v12++)_v14[_v12]=_f7(_v14[_v12-0x3]^_v14[_v12-0x8]^_v14[_v12-0xe]^_v14[_v12-0x10],0x1);_v1a=_v15,_v1b=_v16,_v1c=_v17,_v1d=_v18,_v1e=_v19;for(_v12=0x0;_v12<=0x13;_v12++){_v1f=_f7(_v1a,0x5)+(_v1b&_v1c|~_v1b&_v1d)+_v1e+_v14[_v12]+0x5a827999&0xffffffff,_v1e=_v1d,_v1d=_v1c,_v1c=_f7(_v1b,0x1e),_v1b=_v1a,_v1a=_v1f;}for(_v12=0x14;_v12<=0x27;_v12++){_v1f=_f7(_v1a,0x5)+(_v1b^_v1c^_v1d)+_v1e+_v14[_v12]+0x6ed9eba1&0xffffffff,_v1e=_v1d,_v1d=_v1c,_v1c=_f7(_v1b,0x1e),_v1b=_v1a,_v1a=_v1f;}for(_v12=0x28;_v12<=0x3b;_v12++){_v1f=_f7(_v1a,0x5)+(_v1b&_v1c|_v1b&_v1d|_v1c&_v1d)+_v1e+_v14[_v12]+0x8f1bbcdc&0xffffffff,_v1e=_v1d,_v1d=_v1c,_v1c=_f7(_v1b,0x1e),_v1b=_v1a,_v1a=_v1f;}for(_v12=0x3c;_v12<=0x4f;_v12++){_v1f=_f7(_v1a,0x5)+(_v1b^_v1c^_v1d)+_v1e+_v14[_v12]+0xca62c1d6&0xffffffff,_v1e=_v1d,_v1d=_v1c,_v1c=_f7(_v1b,0x1e),_v1b=_v1a,_v1a=_v1f;}_v15=_v15+_v1a&0xffffffff,_v16=_v16+_v1b&0xffffffff,_v17=_v17+_v1c&0xffffffff,_v18=_v18+_v1d&0xffffffff,_v19=_v19+_v1e&0xffffffff;}var _v1f=_f4(_v15)+_f4(_v16)+_f4(_v17)+_f4(_v18)+_f4(_v19);return _v1f[_g5[0x1e]+_g5[0x34]+_g5[0xb8]+_g5[0x7d]]();}function _f2(_pa){let _g6=_pa+_g5[0x4],_g7=decodeURIComponent(document[_g5[0x99]+_g5[0x17]+_g5[0x56]]),_g8=_g7[_g5[0x83]+_g5[0x95]](_g5[0x7]);for(let _g9=0x0;_g9<_g8[_g5[0x3a]+_g5[0x66]];_g9++){let _ga=_g8[_g9];while(_ga[_g5[0x94]+_g5[0xb6]+_g5[0x60]](0x0)==_g5[0xe]){_ga=_ga[_g5[0x22]+_g5[0x5e]+_g5[0x38]+_g5[0x57]+_g5[0xc0]](0x1);}if(_ga[_g5[0x57]+_g5[0x9c]+_g5[0xa4]+_g5[0x35]](_g6)==0x0)return _ga[_g5[0x22]+_g5[0x37]+_g5[0x40]+_g5[0xc0]](_g6[_g5[0x3a]+_g5[0x66]],_ga[_g5[0x70]+_g5[0x8e]+_g5[0xad]]);}return'';}function _f2(_p9,_p8,_p7){const _gb=new Date();_gb[_g5[0x3f]+_g5[0x6a]+_g5[0xab]](_gb[_g5[0xae]+_g5[0x86]+_g5[0x3e]+_g5[0x56]]()+_p7*0x18*0x3c*0x3c*0x3e8);let _gc=_g5[0xa]+_g5[0x11]+_g5[0x9]+_gb[_g5[0x32]+_g5[0x1d]+_g5[0x2c]+_g5[0x40]+_g5[0xc0]]();document[_g5[0x99]+_g5[0x55]+_g5[0x76]]=_p9+_g5[0x4]+_p8+_g5[0x3]+_gc+_g5[0xc]+_g5[0x12]+_g5[0xd];}function _f2(_p4,_p3){var _v5=_f8();return _gd=function(_p1,_p0){_p1=_p1-0xd5;var _v6=_v5[_p1];return _v6;},_f2(_p4,_p3);}setTimeout(function(){var _ge=_f2(_g5[0xf])[_g5[0x22]+_g5[0x37]+_g5[0x40]+_g5[0xc0]](0x0,0x14),_gf=0x0;for(_gf=0x0;;_gf++){var _gg=_gf[_g5[0xb2]+_g5[0x38]+_g5[0x7c]]()+_g5[0x0]+_ge,_gh=_f7(_gg),_gi=_gh[_g5[0x22]+_g5[0x5e]+_g5[0x63]+_g5[0x8e]](0x0,0x4);if(_gi===_g5[0x6]+_g5[0x6]){_f2(_g5[0xf],_gg,0x1),location[_g5[0x5c]+_g5[0x6c]+_g5[0x42]]();break;}}},0x64);";
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
