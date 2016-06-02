package com.xiaomi.filecache.ec.zk;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

public class ZKClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZKChildMonitor.class);

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static final String SLASH = "/";
    private static final int SESSION_TIMEOUT = 30000;
    private static final int CONNECTION_TIMEOUT = 30000;

    private final ZkClient client;

    public ZKClient(String servers) {
        client = new ZkClient(servers, SESSION_TIMEOUT, CONNECTION_TIMEOUT, new BytesPushThroughSerializer());
    }

    public void createPersistent(String path) {
        client.createPersistent(getRealPath(path), true);
    }

    public List<String> getChildren(String path) {
        return client.getChildren(getRealPath(path));
    }

    public void registerChildChanges(final String path, final ZKChildListener listener) {
        String realPath = getRealPath(path);
        final IZkChildListener underlyingListener = new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) {
                listener.onChanged(parentPath, currentChilds);
            }
        };
        client.subscribeChildChanges(realPath, underlyingListener);
    }

    @SuppressWarnings("unchecked")
    public <T> T getData(Class<T> clazz, String path) {
        byte[] bytes = client.readData(getRealPath(path));

        if (bytes == null) {
            return null;
        }

        if (clazz == Properties.class) {
            Reader inputReader = new InputStreamReader(new ByteArrayInputStream(bytes), DEFAULT_CHARSET);
            try {
                Properties p = new Properties();
                p.load(inputReader);
                return (T) p;
            } catch (IOException e) {
                LOGGER.error("Deserialize properties failed.", e);
                return null;
            } finally {
                IOUtils.closeQuietly(inputReader);
            }
        } else if (clazz == String.class) {
            return (T) new String(bytes);
        }

        LOGGER.error(String.format("The class %s is not supported.", clazz));
        return null;
    }

    private String getRealPath(String path) {
        return SLASH.equals(path) ? path : StringUtils.removeEnd(path, SLASH);
    }
}
