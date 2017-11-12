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
  default = 7
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
    eu-west-1       = "ami-89f6dbef"
    us-east-1       = "ami-42ad7d54"
    ap-southeast-1  = "ami-27cc7d44"
    ap-southeast-2  = "ami-5baeae38"
    us-west-1       = "ami-1a1b457a"
    sa-east-1       = "ami-c51573a9"
    us-west-2       = "ami-2551d145"
    us-gov-west-1   = "ami-a846fcc9"
    eu-central-1    = "ami-4733f928"
    ap-northeast-1  = "ami-86f1b9e1"
  }
}

variable "nat_amis" {
  description = "AMI for Amazon NAT machine"
  default = {
    eu-west-1       = "ami-3760b040"
    us-east-1       = "ami-4c9e4b24"
    ap-southeast-1  = "ami-b082dae2"
    ap-southeast-2  = "ami-996402a3"
    us-west-1       = "ami-2b2b296e"
    sa-east-1       = "ami-b972dba4"
    us-west-2       = "ami-bb69128b"
    us-gov-west-1   = "ami-e8ab1489"
    eu-central-1    = "ami-204c7a3d"
    ap-northeast-1  = "ami-55c29e54"
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
//  default = "79a0dfe0944948a33ab75f6e62335f166e117f3d"
//  default = "4d92536e7381176206e71ee15b5ffe454439920c"
  default = "b7c22f7cfb481d9f957a0adfb7e1e719297b9487"
}

variable "cluster_packages" {
  description = "cluster packages for single master setup"
  default = <<EOF
  ["adminrouter--800844aa8002b626f7e623dbbd683c9fea45b82b",
    "avro-cpp--d4163badf0c83bf9d01f1046802ce9df71496032",
    "boost-libs--a04883bd55bb23fe577a8e60b01bfb0948c18c73",
    "bootstrap--3ef155629151a642136ab832d5778668d9d33aa6",
    "boto--a15478de719282ed240a7f0eb39f7a3c55b8b7a6",
    "check-time--f1609ae2fff1ddedc1d32635dc40e6ab7d2c48b6",
    "cni--21646fdac3ebb193a1a6b2dc4f03b187312dcf27",
    "cosmos--f75b1f45973e46355e71dbd6973dd4054a2c9c61",
    "curl--6583f6b177702ec58b3131d8645362a503186e04",
    "dcos-checks--0178ae7d3846212dd59ad2b0d174116bccb4c38d",
    "dcos-cni--cc00e84332454d6c3b3d94f4752a673ba0faac82",
    "dcos-config--setup_5bef9933906fa3fba6a57d4dc6b42f30c11b59a5",
    "dcos-diagnostics--1e37911ebf7a327c7beff4f820bef2ba2b1d5c04",
    "dcos-history--046380a7b2e385074f8917e85e1f64536b7ad50f",
    "dcos-image--e18e30f94f5786dd37128eee718543866f48f4b0",
    "dcos-image-deps--6144ebb88154b0ad7b373205cd1cceb04bb1fbc8",
    "dcos-integration-test--c17523aa242efeef3d0340c71086c6a8b31bdcdc",
    "dcos-log--3d8342bb425d2876539016fb6250fab021a5fac6",
    "dcos-metadata--setup_5bef9933906fa3fba6a57d4dc6b42f30c11b59a5",
    "dcos-metrics--8745146c535a196b3c4823a6beab7749180c1a8e",
    "dcos-oauth--383c6ea0d983a55804f45f082ccd3e2b8c71869e",
    "dcos-signal--8b57242772836d055e457faa777dd42a11849040",
    "dcos-test-utils--091bc8737cd9a7b2935d3bfd19e4edd2db659d0c",
    "dcos-ui--e516c101057645f9951dcfb123044b061da979b0",
    "dnspython--16379962cc0e56ea1f0b84458336ff8079303618",
    "docker-gc--19fdaeacfc7544c32607a138cc434d00f65b68e0",
    "dvdcli--ee85411e3cb9f0988ed54b5cc0789172b887f12f",
    "erlang--e3bb8b406212554dde3166360c27a08b0d96c38e",
    "exhibitor--5ecf040aa74a1c1f29ab543b4d5a23a738975c5f",
    "flask--6225fa53db5834aca6a3d89582473ca7a87e50f5",
    "java--c6c85f9a41e008de22b4ad43de98ed4426cb5fa0",
    "libevent--e0df071ef6a540fd87454b5034b3a597b201cfb5",
    "libffi--65417d4d857b8daa813a52749413ff825e6ce478",
    "libsodium--6b9df3bf76df0789ddd944235399e0a0f7f041a1",
    "logrotate--b91d427d87bb9a4248e9c71ce9749d168bc95525",
    "marathon--82647df88148fa8f447cf2638efd565657c3b93f",
    "mesos--b561eb0a7d13cda8a36d4fc014e35aefe97b24d9",
    "mesos-dns--600da87080b7634f2380594499004a7ff0b34662",
    "mesos-modules--f0c9e71b8e4bfb02901d17cb82d0a2ce10f98b3c",
    "metronome--aa0bf074aea7376905c9a422b0f2293795701d7d",
    "navstar--d2957bca9d7d37148796cdcfeedd6505c493b9b4",
    "ncurses--2ff075e29c401fe632969613f471a29a6e5ea133",
    "octarine--4e37c062d2f145f9c2ce01d30dadf72c2aac5c4a",
    "openssl--553458f8ece87dc2183d90b24cbc3c7c99262821",
    "pkgpanda-api--6fc0156cb503d1f73bea709bfb9f1b437378d542",
    "pkgpanda-role--9632a14b841dbf07ec8097b3f6e78f53fc030d3b",
    "pytest--362b860cbbef784e02e5ec325821a5b252d32394",
    "python--6fd09648eded002b46ab08542af7cd7910f60f46",
    "python-azure-mgmt-resource--4a906e5cf3ff65a00ac12f9474d4c4cbfedcff78",
    "python-cryptography--b3132edd294edd8cfa73cedff7f3861faf1903b5",
    "python-dateutil--8ff08f7a4a4e0b1677acb5fee613e20f3db0190f",
    "python-docopt--5d19a092d6935624091bc9761145b5feaee88bad",
    "python-gunicorn--2d41b448b1b502831a2f558ce5adbafb0979682d",
    "python-isodate--35d67b64e65914082e0dd1cd2e5d8162a098e2e0",
    "python-jinja2--30a44395cb38f820c90727629dc3d7824e8bad47",
    "python-kazoo--a9775f1c4e3e3e8b8ba570ecf4a5cee8eb5b5347",
    "python-markupsafe--b05d8cac17b8fa69ea013e793613ac544129ded9",
    "python-passlib--e46c65984c8e23fd367d82dda11006b3a111719f",
    "python-pyyaml--333a87d903942a05998a1d9951435079a2ebd7e3",
    "python-requests--adab04d70f9439acbff077e22f58f23130c82eb0",
    "python-retrying--f80ae2ce9367ec9fcf290add26667bb7d88ef4b7",
    "python-tox--6a218314fe969c0848389958d09a343c5c882a16",
    "rexray--da7f17f8a4b772c0bac3f8d289a08abd4ff272b4",
    "six--dd02eb09f0146c22541cad5094a307bd03d1c238",
    "spartan--71b30a170892208e3aa8e61b8fcfc01cd7625a4b",
    "strace--5b7eee0995969d69a0aba586ee5fa58eabfb95d7",
    "teamcity-messages--d4c3298f77d3af91439d404784aabab018217d5f",
    "toybox--bd9cdaf79548a7afd31c572e5f414a1d53f7588c"]EOF
}