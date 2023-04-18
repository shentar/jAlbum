FROM openjdk:11-ea-jre
ENV MY_HOME=/app
RUN mkdir -p $MY_HOME
WORKDIR $MY_HOME
COPY distribute/ /app/

EXPOSE 2148

VOLUME ["/data"]
VOLUME ["/thumbnail"]
VOLUME ["/app/log"]
VOLUME ["/app/config"]

RUN chmod +x /app/jalbum-entrypoint.sh

CMD ["/app/jalbum-entrypoint.sh"]
