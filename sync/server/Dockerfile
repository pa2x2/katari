# The sync server's image. Gradle builds the server first, and its install output is the whole build context, so the
# image is that start script and its jars on a Java runtime:
#   ./gradlew :sync:server:installDist
#   docker buildx build --platform linux/amd64,linux/arm64 -f sync/server/Dockerfile sync/server/build/install/katari-sync-server

# eclipse-temurin:21-jre (Ubuntu 26.04), pinned to the multi-platform index so both architectures get the same build.
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a

COPY . /opt/katari-sync-server

# Uid 1000 is the base image's own unprivileged user. A named volume mounted on /data takes this directory's owner.
RUN mkdir /data && chown 1000:1000 /data

ENV PATH="/opt/katari-sync-server/bin:${PATH}" \
    KATARI_SYNC_DATA_DIR=/data

USER 1000:1000
VOLUME /data
EXPOSE 8080

# The server's info call needs no account or protocol header. The start period covers the first start's schema setup.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
    CMD curl -fsS -o /dev/null "http://127.0.0.1:${KATARI_SYNC_PORT:-8080}/api/info" || exit 1

# The start script execs Java, so `docker stop` reaches the server. Recovery runs beside the serving server:
#   docker exec <container> katari-sync-server reset-password <user>
ENTRYPOINT ["katari-sync-server"]
