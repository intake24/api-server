#!/bin/bash

ssh mech@workcraft.org rm -rf /usr/share/jetty/webapps/scran
scp -r war/ mech@workcraft.org:/usr/share/jetty/webapps/scran
ssh mech@workcraft.org sudo /etc/init.d/jetty restart