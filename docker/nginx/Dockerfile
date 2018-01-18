FROM nginx

LABEL maintainer="Harald Pehl <harald.pehl@gmail.com>"

COPY conf/nginx.conf /etc/nginx/
COPY html /usr/share/nginx/html/

RUN chgrp -R root /var/cache/nginx \
    && chmod -R g+w /var/cache/nginx \
    && chgrp -R root /var/log/nginx \
    && chmod -R g+w /var/log/nginx \
    && chgrp -R root /var/run/ \
    && chmod -R g+w /var/run/

EXPOSE 8080
