#!/usr/bin/env bash
function init {
    echo "init instance"
    export LC_ALL=C.UTF-8
    export LANG=C.UTF-8

    echo "export LC_ALL=C.UTF-8" >> ~/.bashrc
    echo "export LANG=C.UTF-8" >> ~/.bashrc

    apt-get install -y language-pack-UTF-8
}

function install_oracle_java {
    echo "install java"
    wget --no-cookies \
         --no-check-certificate \
         --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
         "http://download.oracle.com/otn-pub/java/jdk/8u25-b17/jdk-8u25-linux-x64.tar.gz" \
         -O /tmp/jdk-8u25-linux-x64.tar.gz
    tar xzf /tmp/jdk-8u25-linux-x64.tar.gz --directory=/usr/local/
    update-alternatives --install "/usr/bin/java" "java" "/usr/local/jdk1.8.0_25/bin/java" 1
    update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/jdk1.8.0_25/bin/javac" 1
    update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/jdk1.8.0_25/bin/javaws" 1
    update-alternatives --set "java" "/usr/local/jdk1.8.0_25/bin/java"
    update-alternatives --set "javac" "/usr/local/jdk1.8.0_25/bin/javac"
    update-alternatives --set "javaws" "/usr/local/jdk1.8.0_25/bin/javaws"
    export JAVA_HOME=/usr/local/jdk1.8.0_25/
    echo "export JAVA_HOME=$JAVA_HOME" >> ~/.bashrc
}

function waited_until_marathon_is_running {
    until $(curl --output /dev/null --silent --head --fail http://${internal_master_lb_dns_name}:8080/v2/info); do
        echo "waiting for marathon"
        sleep 5
    done
}

function export_public_ip {
    echo "resolve public ip"
    export PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-hostname)
    echo "Public IP is: $PUBLIC_IP"
}

function export_dns_nameserver {
    echo "resolve dns server"
    apt-get install -y jq

    until [ -n "$DNS_NAMESERVER" ]; do
        sleep 5
        echo "waiting until name server is ready"
        export DNS_NAMESERVER=$(curl -s http://${internal_master_lb_dns_name}:8080/v2/info | jq '.leader' | sed 's/\"//' | sed 's/\:8080"//')
    done

    echo "Nameserver is: $DNS_NAMESERVER"
}

function install_openvpnas {
    echo "Install openvpnas"
    wget -O /tmp/openvpn-as-2.0.25-Ubuntu14.amd_64.deb http://swupdate.openvpn.org/as/openvpn-as-2.0.25-Ubuntu14.amd_64.deb
    dpkg -i /tmp/openvpn-as-2.0.25-Ubuntu14.amd_64.deb
    rm -f /tmp/openvpn-as-2.0.25-Ubuntu14.amd_64.deb

    service openvpnas restart
}


function setup_openvpnas {
    echo "Setup openvpnas"
    echo "Add openvpn admin ${admin_user}"
    useradd ${admin_user}
    echo "${admin_user}:${admin_pw}" | chpasswd
    /usr/local/openvpn_as/scripts/sacli -u ${admin_user} -k prop_superuser -v true UserPropPut

    /usr/local/openvpn_as/scripts/sacli -u openvpn UserPropDelAll

    /usr/local/openvpn_as/scripts/sacli -k vpn.client.routing.reroute_dns -v custom ConfigPut
    /usr/local/openvpn_as/scripts/sacli -k vpn.general.osi_layer -v 3 ConfigPut
    /usr/local/openvpn_as/scripts/sacli -k vpn.server.routing.gateway_access -v true ConfigPut
    /usr/local/openvpn_as/scripts/sacli -k vpn.client.routing.reroute_gw -v false ConfigPut
    /usr/local/openvpn_as/scripts/sacli -k vpn.server.routing.private_network.0 -v ${vpc_subnet_range} ConfigPut

    waited_until_marathon_is_running
    export_dns_nameserver
    /usr/local/openvpn_as/scripts/sacli -k vpn.server.dhcp_option.dns.0 -v $DNS_NAMESERVER ConfigPut

    export_public_ip
    /usr/local/openvpn_as/scripts/sacli -k host.name -v $PUBLIC_IP ConfigPut

    service openvpnas restart
}

init
install_oracle_java
install_openvpnas
setup_openvpnas
