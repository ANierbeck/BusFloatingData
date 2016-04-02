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
  default = "m3.large"
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
    us-west-1       = "ami-52e69432"
    ap-northeast-1  = "ami-962c39f8"
    ap-northeast-2  = "ami-03a76e6d"
    us-gov-west-1   = "ami-0f3c806e"
    us-west-2       = "ami-4f4ba32f"
    us-east-1       = "ami-2c393546"
    sa-east-1       = "ami-a49915c8"
    ap-southeast-2  = "ami-74dcfc17"
    eu-west-1       = "ami-c346c2b0"
    eu-central-1    = "ami-cb8d6ba4"
    ap-southeast-1  = "ami-3b8f4558"
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
    us-west-1       = "ami-21166541"
    ap-northeast-1  = "ami-9b414ef5"
    us-west-2       = "ami-4e57bb2e"
    us-east-1       = "ami-db5269b1"
    sa-east-1       = "ami-6138ba0d"
    ap-southeast-2  = "ami-f32c0d90"
    eu-west-1       = "ami-75833a06"
    eu-central-1    = "ami-33896d5c"
    ap-southeast-1  = "ami-240bc347"
  }
}

variable "ubuntu_amis" {
  description = "Ubuntu AMIs for regions"
  default = {
    us-west-1       = "ami-972959f7"
    ap-northeast-1  = "ami-80696aee"
    us-west-2       = "ami-6b2cce0b"
    us-east-1       = "ami-d55b69bf"
    sa-east-1       = "ami-ffe46693"
    ap-southeast-2  = "ami-6831170b"
    eu-west-1       = "ami-36c97545"
    eu-central-1    = "ami-2ae1fb46"
    ap-southeast-1  = "ami-7902cb1a"
  }
}

variable "cluster_packages" {
  description = "cluster packages for single master setup"
  default = <<EOF
    [
      "dcos-config--setup_4597f57ee78b805648fda7ebc52e57aa209351c9",
      "dcos-metadata--setup_4597f57ee78b805648fda7ebc52e57aa209351c9"
    ]
EOF
}

//variable "cluster_packages" {
//  description = "cluster packages for multi master setup"
//  default = <<EOF
//    [
//      "dcos-config--setup_39bcd04b14a990a870cdff4543566e78d7507ba5",
//      "dcos-metadata--setup_39bcd04b14a990a870cdff4543566e78d7507ba5"
//    ]
//EOF
//}
