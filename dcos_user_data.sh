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
useradd dcos
