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

###############################
### CONFIGURABLE PARAMETERS ###
###############################

variable "stack_name" {
  description = "DCOS stack name"
  default = "DCOS"
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
  descpiption = "The IP range of the VPC subnet"
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
    us-west-1       = "ami-d8770bb8"
    ap-northeast-1  = "ami-e304148d"
    ap-northeast-2  = "ami-131dd47d"
    us-gov-west-1   = "ami-cf19a5ae"
    us-west-2       = "ami-4f7f8a2f"
    us-east-1       = "ami-7a627510"
    sa-east-1       = "ami-d75bd4bb"
    ap-southeast-2  = "ami-a184a7c2"
    eu-west-1       = "ami-3b941448"
    eu-central-1    = "ami-e13fde8e"
    ap-southeast-1  = "ami-52a07531"
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

variable "vpn_amis" {
  description = "VPN AMIs for regions"
  default = {
    us-west-1       = "ami-20423a40"
    ap-northeast-1  = "ami-f33bda92"
    us-west-2       = "ami-a1b847c1"
    us-east-1       = "ami-7ab25917"
    sa-east-1       = "ami-1356de7f"
    ap-southeast-2  = "ami-f3331c90"
    eu-west-1       = "ami-c61185b5"
    eu-central-1    = "ami-51e30f3e"
    ap-southeast-1  = "ami-47518724"
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
    eu-west-1       = "ami-564e0b36"
    eu-central-1    = "ami-e3f0198c"
    ap-southeast-1  = "ami-eda0738e"
  }
}

variable "filebeat_download_url" {
  description = "filebeats download url"
  default = "https://download.elastic.co/beats/filebeat/filebeat-1.2.1-x86_64.tar.gz"
}

variable "filebeats_configuration" {
  description = "filebeats configuration file"
  default = <<EOF
filebeat:
  prospectors:
    - paths:
        - "/var/lib/mesos/slave/slaves/*/frameworks/*/executors/*/runs/latest/stdout"
        - "/var/lib/mesos/slave/slaves/*/frameworks/*/executors/*/runs/latest/stderr"
      fields:
        node: "###NODE_TYPE###"
output:
  logstash:
    hosts: ["logstash.marathon.mesos:5044"]
EOF
}

variable "authentication_enabled" {
  description = "authentication_enabled"
  default = true
}

variable "bootstrap_id" {
  description = "bootstrap id that is used to download the bootstrap files"
  default = "3a2b7e03c45cd615da8dfb1b103943894652cd71"
}


variable "cluster_packages" {
  description = "cluster packages for single master setup"
  default = <<EOF
    [
      "dcos-config--setup_537afa008db7ba8f99ce73f9c0ef425fa61d3454",
      "dcos-metadata--setup_537afa008db7ba8f99ce73f9c0ef425fa61d3454"
    ]EOF
}

//variable "cluster_packages" {
//  description = "cluster packages for multi master setup"
//  default = <<EOF
//    [
//      "dcos-config--setup_b9372277c9fedaca077d7638e6e445af062d1d86",
//      "dcos-metadata--setup_b9372277c9fedaca077d7638e6e445af062d1d86"
//    ]EOF
//}
