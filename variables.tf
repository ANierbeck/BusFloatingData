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

###############################
### CONFIGURABLE PARAMETERS ###
###############################

variable "stack_name" {
  description = "DCOS stack name"
  default = "dcos"
}

variable "slave_instance_count" {
  description = "Number of slave nodes to launch"
  default = 2
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

variable "nat_ami" {
  description = "AMI for Amazon NAT machine"
  default = "ami-204c7a3d"
}

variable "instance_ami" {
  description = "AMI for CoreOS machine"
  default = "ami-bececaa3"
}

variable "master_instance_type" {
  description = "Default instance type for masters"
  default = "m4.large"
}

variable "slave_instance_type" {
  description = "Default instance type for slaves"
  default = "m4.large"
}

variable "public_slave_instance_type" {
  description = "Default instance type for public slaves"
  default = "m4.large"
}

variable "vpc_subnet_range" {
  descpiption = "The IP range of the VPC subnet"
  default = "10.0.0.0/16"
}

variable "bootstrap_repo_root" {
  descpiption = "Root address of the bootstrap script"
  default = "https://downloads.mesosphere.io/dcos/stable"
}

variable "master_instance_count" {
  description = "Amount of requested Masters"
  default = 1
}

variable "master_quorum_count" {
  description = "Quorum count"
  default = 1
}

variable "private_subnet_range" {
  description = "Private Subnet IP range"
  default = "10.0.0.0/22"
}

variable "public_subnet_range" {
  description = "Public Subnet IP range"
  default = "10.0.4.0/22"
}

variable "fallback_dns" {
  description = "Fallback DNS IP"
  default = "10.0.0.2"
}
