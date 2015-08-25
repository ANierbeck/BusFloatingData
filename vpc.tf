resource "aws_vpc" "dcos" {
  cidr_block = "${var.vpc_subnet_range}"
  enable_dns_support = true
  enable_dns_hostnames = true

  tags {
    Name = "${var.stack_name}"
    Application = "${var.stack_name}"
    Network = "Public"
  }
}
