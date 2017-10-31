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
  default = 9
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
  default = "4d92536e7381176206e71ee15b5ffe454439920c"
}

variable "cluster_packages" {
  description = "cluster packages for single master setup"
  default = <<EOF
  ["adminrouter--1166a3736442e7963a68d1d644bf5f54ca3cb01d",
    "avro-cpp--9cb0ee14e3cd5bbdb171efcc72a84d16862ea02d",
    "boost-libs--8d515c2f703c666ae1b6c5ccc35cc0f8fa36677f",
    "bootstrap--c1bc86593e212cf9fe83db2246bacd129a6b3adc",
    "boto--3890cb2817c00b874ba033abe784b5b343caa3c7",
    "check-time--79e3f6ab99125471e1d94d5f6bc0fea88446831c",
    "cni--7a8572e385c3f5262945c52c8003d1bbb22cf7aa",
    "cosmos--e84c5bf3259405df90d682536ba445cc4839a324",
    "curl--17866a8ae9305826aa5f357a09db2c1f2b2c2ad0",
    "dcos-checks--8fd33919e6f163dba1bd13e4c7e4e0523919a719",
    "dcos-cni--12a77c1e9bebd4cbd600524a864c2bd8483330d3",
    "dcos-config--setup_d32e054e113b14d97841dd13b974a222976a8d62",
    "dcos-diagnostics--e3b557b0ec8e98617d0cd0fdf136ef9dded96316",
    "dcos-history--23de88ddc1a5f9018dd11b279c5be6a768a18de4",
    "dcos-image--df630d8e930d6650ce3d0ade519660142233d862",
    "dcos-image-deps--81d23d00b1acddb316c9b15fd8499c2b10f6b697",
    "dcos-integration-test--9ec173650d4e73ba494603324e7583d23970e4b8",
    "dcos-log--d2af4b1a47d3755a51823e95fbc6c366cf0f9269",
    "dcos-metadata--setup_d32e054e113b14d97841dd13b974a222976a8d62",
    "dcos-metrics--2a26c0b50b0b6564f86c48d50aa86f681c9af93c",
    "dcos-oauth--445bb1388670981c6acc667b2529fc32d4c1fbd4",
    "dcos-signal--4366023212ea49a64c5c9aef1965e5a3133c4b61",
    "dcos-test-utils--1066d896d25f4c1e3f6d9a5e7f9c1c6e8c675bb7",
    "dcos-ui--cc2e3d26537ea190efacd6f899dd4cc2210d45b7",
    "dnspython--0be432372a3820eafcfa66975943c9536dbe1164",
    "docker-gc--89f5535aea154dca504f84cd60eac6f61836aef9",
    "dvdcli--ee85411e3cb9f0988ed54b5cc0789172b887f12f",
    "erlang--d693172f6f033707c7f07ff78fc18ac543d66b41",
    "exhibitor--c3e48bbae19c0ed9c30d7f9396305d1e77130658",
    "flask--6d0f985ad677e8422c7190cbe207424acd813c3b",
    "java--ce5ff19502fca31eaf4a9af86d50a10a8c212a5b",
    "libevent--05dc18bc0ab7434b2738318c5ebaa2e61a311f50",
    "libffi--0e5b99b94f296b2a9a1b75e9fe5f74f5446f5e9b",
    "libsodium--e7056355f1fe160ade83aac0d11352a2bf3844e6",
    "logrotate--877aece1fd506af3b9167b6938c316adfa79d4f5",
    "marathon--accdc43bafeca02da1be340baba4b55011eadf63",
    "mesos--0677ce2b7d2e8c45091f6481884542f1f765c3d5",
    "mesos-dns--600da87080b7634f2380594499004a7ff0b34662",
    "mesos-modules--1f5c4860450949db92ed27326c3146526041e681",
    "metronome--2ec6f56be44ed822e7228cb66c4dae6a78345789",
    "navstar--c66f92f01d837433de3e2b19d221c64d26cc54b1",
    "ncurses--030fd6b08ed46a7ecce001c36901f5b4ad5d2af5",
    "octarine--4e37c062d2f145f9c2ce01d30dadf72c2aac5c4a",
    "openssl--44777d19d54a3c33cc19543f2201cb20bf085d98",
    "pkgpanda-api--30cb1e68f92ed5d4b89d57ca526f8a69b44132c8",
    "pkgpanda-role--612a6734567cc0c7c2ae1d508f03172f4bc7beed",
    "pytest--5e26c8ed9fd2c325672d56fe558299bfbd0f7018",
    "python--5a4285ff7296548732203950bf73d360ea67f6ab",
    "python-azure-mgmt-resource--26cbe8349f3fe139f7dc8bff7f0cb735382314fc",
    "python-cryptography--0d83d8afef4a8faddf0d8b713619d9d76e510a9e",
    "python-dateutil--519201adebeba186049ecd79a9f358f614173b10",
    "python-docopt--0af809c220a922f7f6c58f15beafebaa043477c7",
    "python-gunicorn--2ceb53716237da0736f67f4004682083f6ac68e1",
    "python-isodate--c9efb5859a0cfb06d82f25220cc5b387914af85d",
    "python-jinja2--601a1443aa4c649ab1da10c2a6d7a4477a263fb3",
    "python-kazoo--0ff8e6ef528f58c6f36f0a9df6dc27d3871e5c27",
    "python-markupsafe--1388c95920b4eb920c7a753d620a1ad07fc8b64d",
    "python-passlib--4691268be760073188b555dc436f836c6706b37a",
    "python-pyyaml--d8a775d6e43da5eb239af5cccdf1d3fceeb0335f",
    "python-requests--db0474fab16019ba29a609a354285f221c1a2859",
    "python-retrying--37dd25bf69bcbefe0c50139085d6bb2e22ccf439",
    "python-tox--322c468e2a75c5b143cb06af460b5e801ee34342",
    "rexray--da7f17f8a4b772c0bac3f8d289a08abd4ff272b4",
    "six--93734bac9907087744815f9cb5b6152e9a198fae",
    "spartan--c3d8005b1340bcbc3a00496861745b2d0bb2d697",
    "strace--9be573456909e3931a890785eb6474af7e0dcce4",
    "teamcity-messages--073793b16cf369e58ebdb6348b93ed14b0e5e59a",
    "toybox--0c49f879bfe2f99e6f99b397136894fa5096fa0c"]EOF
}