FROM nginx

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

COPY conf/nginx.conf /etc/nginx/
COPY html /usr/share/nginx/html/

RUN adduser --disabled-password --uid 1001 --gid 0 --gecos "Nginx Model Graph" nginxmg \
    && chown -R nginxmg:root /var/cache/nginx \
    && chmod -R u+w,g+w /var/cache/nginx \
    && chown -R nginxmg:root /var/log/nginx \
    && chmod -R u+w,g+w /var/log/nginx \
    && chown -R nginxmg:root /var/run/ \
    && chmod -R u+w,g+w /var/run/

USER 1001

EXPOSE 8080
