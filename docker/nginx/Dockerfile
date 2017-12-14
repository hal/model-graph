FROM nginx

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

COPY conf/nginx.conf /etc/nginx/
COPY html /usr/share/nginx/html/

RUN chmod 777 /var/cache/nginx

USER nginx

EXPOSE 8080
