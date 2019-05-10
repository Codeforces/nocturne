/*
 * Copyright by Mike Mirzayanov
 */
package bloggy.captions.dao.impl;

import com.codeforces.commons.text.StringUtil;
import com.google.inject.Singleton;
import freemarker.cache.SoftCacheStorage;
import freemarker.template.Configuration;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.nocturne.main.ApplicationContext;
import bloggy.captions.TemplateEngineConfigurations;
import bloggy.captions.dao.CaptionDao;
import bloggy.captions.model.Caption;
import bloggy.dao.impl.ApplicationDaoImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class CaptionDaoImpl extends ApplicationDaoImpl<Caption> implements CaptionDao {
    private static final Logger logger = Logger.getLogger(CaptionDaoImpl.class);
    private static final ConcurrentMap<String, String> utf8Cache = new ConcurrentHashMap<>();

    @Override
    public String shaHex(String s) {
        if (s == null) {
            return null;
        }

        boolean ascii = true;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c > 127) {
                ascii = false;
                break;
            }
        }

        if (ascii) {
            return DigestUtils.sha1Hex(s);
        } else {
            return shaHexUsingDatabase(s);
        }
    }

    private String shaHexUsingDatabase(String s) {
        String result = utf8Cache.get(s);

        if (StringUtil.isEmpty(result)) {
            result = getJacuzzi().findString("SELECT CONVERT(SHA1(?) USING utf8)", s);
            utf8Cache.putIfAbsent(s, result);
        }

        return result;
    }

    @Override
    public Caption find(String shortcutSha1, String locale) {
        return findOnlyBy("shortcutSha1=? AND locale=?", shortcutSha1, locale);
    }

    @Override
    public void save(Caption object) {
        if (StringUtil.isNotEmpty(object.getShortcut())) {
            object.setShortcutSha1(getJacuzzi().findString("SELECT CONVERT(SHA1(?) USING utf8)",
                    object.getShortcut()));
        }

        Caption prev = find(object.getShortcutSha1(), object.getLocale());

        if (prev != null) {
            object.setId(prev.getId());
            object.setShortcut(prev.getShortcut());
        }

        if (prev == null) {
            prev = find(object.getShortcutSha1(), ApplicationContext.getInstance().getDefaultLocale().toString());

            if (prev != null) {
                object.setShortcut(prev.getShortcut());
            }
        }

        super.save(object);
        clearCache();
    }

    private void clearCache() {
        if (ApplicationContext.getInstance().isDebug()) {
            clearTemplateCache();
        }

        logger.warn("Captions cache has been cleared.");
    }

    private void clearTemplateCache() {
        for (Configuration configuration : TemplateEngineConfigurations.get()) {
            configuration.clearEncodingMap();
            configuration.clearSharedVariables();
            configuration.clearTemplateCache();
            configuration.setCacheStorage(new SoftCacheStorage());
        }
    }

    @Override
    public void insert(Caption caption) {
        caption.setShortcutSha1(shaHex(caption.getShortcut()));

        try {
            super.insert(caption);
        } catch (Exception ignored) {
            // No operations.
        } finally {
            clearCache();
        }
    }
}
