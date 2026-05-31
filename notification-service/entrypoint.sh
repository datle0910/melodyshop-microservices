#!/bin/sh
set -eu

JAVA_TOOL_OPTIONS=
export JAVA_TOOL_OPTIONS

exec java \
    -DsocksProxyHost= \
    -DsocksProxyPort= \
    -Djava.net.useSystemProxies=false \
    -Djava.net.preferIPv4Stack=true \
    -Dftp.proxyHost= \
    -Dftp.proxyPort= \
    -Dhttp.proxyHost= \
    -Dhttp.proxyPort= \
    -Dhttps.proxyHost= \
    -Dhttps.proxyPort= \
    -Dmail.smtp.socks.host= \
    -Dmail.smtps.socks.host= \
    -Dmail.socks.host= \
    org.springframework.boot.loader.launch.JarLauncher
