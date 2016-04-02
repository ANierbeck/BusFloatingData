#!/usr/bin/env bash
function init {
    export LANGUAGE=en_US.UTF-8
    export LANG=en_US.UTF-8
    export LC_ALL=en_US.UTF-8
    locale-gen en_US.UTF-8
    dpkg-reconfigure locales

    mkdir -p /opt/mesosphere/dcos-cli
}

function install_dcos_cli {
    curl -s --output /tmp/get-pip.py https://bootstrap.pypa.io/get-pip.py
    python /tmp/get-pip.py
    pip install virtualenv
    curl -s --output /tmp/dcos_cli_install.sh https://downloads.mesosphere.com/dcos-cli/install.sh
    chmod +x /tmp/dcos_cli_install.sh
    yes | /tmp/dcos_cli_install.sh /opt/mesosphere/dcos-cli http://${internal_master_lb_dns_name}
    ln -s /opt/mesosphere/dcos-cli/bin/dcos /usr/local/bin/dcos
}

function install_oracle_java {
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.tar.gz" -O /tmp/jdk-8u25-linux-x64.tar.gz
    tar xzf /tmp/jdk-8u25-linux-x64.tar.gz --directory=/usr/local/
    update-alternatives --install "/usr/bin/java" "java" "/usr/local/jdk1.8.0_25/bin/java" 1
    update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/jdk1.8.0_25/bin/javac" 1
    update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/jdk1.8.0_25/bin/javaws" 1
    update-alternatives --set "java" "/usr/local/jdk1.8.0_25/bin/java"
    update-alternatives --set "javac" "/usr/local/jdk1.8.0_25/bin/javac"
    update-alternatives --set "javaws" "/usr/local/jdk1.8.0_25/bin/javaws"
}

function set_dcos_nameserver {
    apt-get install -y jq
    echo nameserver $(curl -s http://${internal_master_lb_dns_name}:8080/v2/info | jq '.leader' | sed 's/\"//' | sed 's/\:8080"//') &> /etc/resolvconf/resolv.conf.d/head
    resolvconf -u
}

function waited_for_running_cluster {
    touch /opt/mesosphere/waited_for_running_cluster
    until $(curl --output /dev/null --silent --head --fail http://${internal_master_lb_dns_name}:8080/ping); do
        sleep 5
    done
    rm /opt/mesosphere/waited_for_running_cluster
}

init
install_oracle_java #need for same commandline extension like kafka
waited_for_running_cluster
set_dcos_nameserver
install_dcos_cli
