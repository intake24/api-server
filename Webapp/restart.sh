#!/bin/bash

/etc/init.d/jetty stop
rm -r /usr/share/jetty/webapps/scran/
cp -r war/ /usr/share/jetty/webapps/scran
/etc/init.d/jetty start
