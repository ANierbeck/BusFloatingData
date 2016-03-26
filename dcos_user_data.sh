#!/usr/bin/env bash
export LANGUAGE=en_US.UTF-8
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
locale-gen en_US.UTF-8
sudo dpkg-reconfigure locales
curl --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
python /tmp/get-pip.py
pip install virtualenv
curl --output /tmp/dcos_cli_install.sh https://downloads.mesosphere.com/dcos-cli/install.sh
chmod +x /tmp/dcos_cli_install.sh
mkdir -p /opt/mesosphere/dcos-cli
yes | /tmp/dcos_cli_install.sh /opt/mesosphere/dcos-cli http://${internal_master_lb_dns_name}
ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/local/bin/dcos
wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.tar.gz" -O /tmp/jdk-8u25-linux-x64.tar.gz
tar xzf /tmp/jdk-8u25-linux-x64.tar.gz --directory=/usr/local/
sudo update-alternatives --install "/usr/bin/java" "java" "/usr/local/jdk1.8.0_25/bin/java" 1
sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/jdk1.8.0_25/bin/javac" 1
sudo update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/jdk1.8.0_25/bin/javaws" 1
sudo update-alternatives --set "java" "/usr/local/jdk1.8.0_25/bin/java"
sudo update-alternatives --set "javac" "/usr/local/jdk1.8.0_25/bin/javac"
sudo update-alternatives --set "javaws" "/usr/local/jdk1.8.0_25/bin/javaws"
