variable "aws_access_key" {
  description = "AWS Access Key"
}

variable "aws_secret_key" {
  description = "AWS Secret Key"
}

variable "aws_region" {
  description = "AWS Region to launch configuration in"
}

variable "ssh_public_key" {
  description = "SSH public key to give SSH access"
}

variable "openvpn_admin_user" {
  description = "Username of the open VPN Admin User"
}

variable "openvpn_admin_pw" {
  description = "Password of the open VPN Admin User"
}

variable "exhibitor_uid" {
  description = "Unique Intentifier"
}


###############################
### CONFIGURABLE PARAMETERS ###
###############################

variable "stack_name" {
  description = "DCOS stack name"
  default = "DCOS"
}

variable "elb_version" {
  description = "Loadbalancer Version"
  default = ""
}

variable "slave_instance_count" {
  description = "Number of slave nodes to launch"
  default = 6
}

variable "public_slave_instance_count" {
  description = "Number of public slave nodes to launch"
  default = 1
}

variable "admin_location" {
  description = "The IP range to whitelist for admin access. Must be a valid CIDR."
  default = "0.0.0.0/0"
}

##################
### PARAMETERS ###
##################

variable "aws_availability_zone" {
  description = "AWS Secret Key"
  default = "eu-central-1b"
}

variable "dcos_gateway_instance_type" {
  description = "Default instance type for masters"
  default = "m3.medium"
}

variable "vpn_instance_type" {
  description = "Default instance type for masters"
  default = "m3.medium"
}

variable "master_instance_type" {
  description = "Default instance type for masters"
  default = "m3.xlarge"
}

variable "slave_instance_type" {
  description = "Default instance type for slaves"
  default = "m3.xlarge"
}

variable "public_slave_instance_type" {
  description = "Default instance type for public slaves"
  default = "m3.xlarge"
}

variable "vpc_subnet_range" {
  #descpiption = "The IP range of the VPC subnet"
  default = "10.0.0.0/16"
}

variable "master_instance_count" {
  description = "Amount of requested Masters"
  default = 1
  #when override number of instances please use an other cluster_packages (see below)
}

variable "private_subnet_range" {
  description = "Private Subnet IP range"
  default = "10.0.0.0/22"
}

variable "public_subnet_range" {
  description = "Public Subnet IP range"
  default = "10.0.4.0/24"
}

variable "master_subnet_range" {
  description = "Master Subnet IP range"
  default = "10.0.5.0/24"
}

variable "fallback_dns" {
  description = "Fallback DNS IP"
  default = "10.0.0.2"
}

variable "coreos_amis" {
  description = "AMI for CoreOS machine"
  default = {
    us-west-1       = "ami-1a1b457a"
    ap-northeast-1  = "ami-86f1b9e1"
    us-gov-west-1   = "ami-a846fcc9"
    us-west-2       = "ami-2551d145"
    us-east-1       = "ami-42ad7d54"
    sa-east-1       = "ami-c51573a9"
    ap-southeast-2  = "ami-5baeae38"
    eu-west-1       = "ami-89f6dbef"
    eu-central-1    = "ami-4733f928"
    ap-southeast-1  = "ami-27cc7d44"
  }
}

variable "nat_amis" {
  description = "AMI for Amazon NAT machine"
  default = {
    us-west-1       = "ami-2b2b296e"
    ap-northeast-1  = "ami-55c29e54"
    us-gov-west-1   = "ami-bb69128b"
    us-west-2       = "ami-00ebfc61"
    us-east-1       = "ami-4c9e4b24"
    sa-east-1       = "ami-b972dba4"
    ap-southeast-2  = "ami-996402a3"
    eu-west-1       = "ami-3760b040"
    eu-central-1    = "ami-204c7a3d"
    ap-southeast-1  = "ami-b082dae2"
  }
}

variable "dns_domainnames" {
  description = "DNS Names for regions"
  default = {
    us-west-1       = "compute.internal"
    ap-northeast-1  = "compute.internal"
    us-gov-west-1   = "compute.internal"
    us-west-2       = "compute.internal"
    us-east-1       = "ec2.internal"
    sa-east-1       = "compute.internal"
    ap-southeast-2  = "compute.internal"
    eu-west-1       = "compute.internal"
    eu-central-1    = "compute.internal"
    ap-southeast-1  = "compute.internal"
  }
}

variable "ubuntu_amis" {
  description = "Ubuntu AMIs for regions"
  default = {
    us-west-1       = "ami-1dec736e"
    ap-northeast-1  = "ami-1707ec76"
    us-west-2       = "ami-e97d8789"
    us-east-1       = "ami-304b8e5d"
    sa-east-1       = "ami-8d9913e1"
    ap-southeast-2  = "ami-62e3ca01"
    eu-west-1       = "ami-1dec736e"
    eu-central-1    = "ami-e3f0198c"
    ap-southeast-1  = "ami-eda0738e"
  }
}

variable "authentication_enabled" {
  description = "authentication_enabled"
  default = true
}

variable "dcos_base_download_url" {
  description = "base url that is used to download the dcos"
  default = "https://downloads.dcos.io/dcos/stable"
}

variable "bootstrap_id" {
  description = "bootstrap id that is used to download the bootstrap files"
//  default = "405172d16eaff8798d6b090dac99b51a8a9004d7"
//  default = "58fd0833ce81b6244fc73bf65b5deb43217b0bd7"
  default = "79a0dfe0944948a33ab75f6e62335f166e117f3d"
}

variable "cluster_packages" {
  description = "cluster packages for single master setup"
  default = <<EOF
  ["3dt--caa28740ba55b7f6de50ab574e91a3f087701e7a",
    "adminrouter--a4c1e2c311b66185e3fc26350ef55086a4434b3c",
    "avro-cpp--b705160facaef4071e6e1999d4a3f10b6bebddfc",
    "boost-libs--2015ccb58fb756f61c02ee6aa05cc1e27459a9ec",
    "bootstrap--d8cdde7d671f2ef25087c2941e28d60049f168dd",
    "boto--6344d31eef082c7bd13259b17034ea7b5c34aedf",
    "check-time--be7d0ba757ec87f9965378fee7c76a6ee5ae996d",
    "cni--e48337da39a8cd379414acfe0da52a9226a10d24",
    "cosmos--e51eff0287fa62a5c3f119bf49596f616eafad72",
    "curl--fc3486c43f98e63f9b12675f1356e8fe842f26b0",
    "dcos-config--setup_693350afac89c67023b39be99664109dde17dbdd",
    "dcos-history--f059725ebe14a980c1b7d889055d78c56720d737",
    "dcos-image--e42f099fbe4d35adb7ae10316e6e88f308a08397",
    "dcos-image-deps--83584fd868e5b470f7cf754424a9a75b328e9b68",
    "dcos-integration-test--3acd68afab37740ef5e19c1658f7c7d6e3ae073a",
    "dcos-log--4d630df863228f38c6333e44670b4c4b20a74832",
    "dcos-metadata--setup_693350afac89c67023b39be99664109dde17dbdd",
    "dcos-metrics--3f9c56b0c3aef016427b723a7d2e4e6afdf9c04e",
    "dcos-oauth--0079529da183c0f23a06d2b069721b6fa6cc7b52",
    "dcos-signal--5633dc8da7e864cb34e3d29ed13e6756c7a6df94",
    "dcos-ui--7693dca6567e31cc107b765ce6612ae48a417528",
    "dnspython--0f833eb9a8abeba3179b43f3a200a8cd42d3795a",
    "docker-gc--59a98ed6446a084bf74e4ff4b8e3479f59ea8528",
    "dvdcli--5374dd4ffb519f1dcefdec89b2247e3404f2e2e3",
    "erlang--a9ee2530357a3301e53056b36a93420847b339a3",
    "exhibitor--6be3f543dbed3abbc22218fd703e52609bc49a67",
    "flask--26d1bcdb2d1c3dcf1d2c03bc0d4f29c86d321b21",
    "java--cd5e921ce66b0d3303883c06d73a657314044304",
    "libevent--208be855d2be29c9271a7bd6c04723ff79946e02",
    "libffi--83ce3bd7eda2ef089e57efd2bc16c144d5a1f094",
    "libsodium--9ff915db08c6bba7d6738af5084e782b13c84bf8",
    "logrotate--7f7bc4416d3ad101d0c5218872858483b516be07",
    "marathon--8f59f94aaa21adbe497e0f49a8d0bb41515968db",
    "mesos--8672838b73e866c786446d1a4d7afce701f7df62",
    "mesos-dns--f8c80e9bffa1fe238711f82fa06006de8715cceb",
    "mesos-modules--6680e631c9689dd33f1115bdae3d31fafe70051f",
    "metronome--6eafabd93182e8844c72ce19690e93d2b7926931",
    "navstar--0b141af667446bbe42069fbdba130276f872061c",
    "ncurses--d889894b71aa1a5b311bafef0e85479025b4dacb",
    "octarine--dada18ca28c1b7948ed895580cc0a59d5b3c5583",
    "openssl--b01a32a42e3ccba52b417276e9509a441e1d4a82",
    "pkgpanda-api--1acf427af3dd51b7bd56176bce3e8a6580489219",
    "pkgpanda-role--f8a749a4a821476ad2ef7e9dd9d12b6a8c4643a4",
    "pytest--78aee3e58a049cdab0d266af74f77d658b360b4f",
    "python--b7a144a49577a223d37d447c568f51330ee95390",
    "python-azure-mgmt-resource--03c05550f43b0e7a4455c33fe43b0deb755d87f0",
    "python-cryptography--4184767c68e48801dd394072cb370c610a05029d",
    "python-dateutil--fdc6ff929f65dd0918cf75a9ad56704683d31781",
    "python-docopt--beba78faa13e5bf4c52393b4b82d81f3c391aa65",
    "python-gunicorn--a537f95661fb2689c52fe12510eb0d01cb83af60",
    "python-isodate--40d378c688e6badfd16676dd8b51b742bfebc8d5",
    "python-jinja2--7450f5ae5a822f63f7a58c717207be0456df51ed",
    "python-kazoo--cb7ce13a1068cd82dd84ea0de32b529a760a4bdd",
    "python-markupsafe--dd46d2a3c58611656a235f96d4adc51b2a7a590e",
    "python-passlib--802ec3605c0b82428fedba60983b1bafaa036bb8",
    "python-pyyaml--81dd44cc4a24db7cefa7016c6586a131acf279c3",
    "python-requests--1b2cadbd3811cc0c2ee235ce927e13ea1d6af41d",
    "python-retrying--eb7b8bac133f50492b1e1349cbe77c3e38bd02c3",
    "python-tox--07244f8a939a10353634c952c6d88ec4a3c05736",
    "rexray--f07795e2c10f9a1a27de9d8e67ab171029db2e1d",
    "six--f06424b68523c4dfa2a7c3e7475d479f3d361e42",
    "spartan--daba6f5a190e67874b3aa852601ba039ecbab039",
    "strace--7d01796d64994451c1b2b82d161a335cbe90569b",
    "teamcity-messages--e623a4d86eb3a8d199cefcc240dd4c5460cb2962",
    "toybox--f235594ab8ea9a2864ee72abe86723d76f92e848"]EOF
}
