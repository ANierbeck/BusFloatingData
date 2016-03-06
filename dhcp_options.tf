resource "aws_vpc_dhcp_options" "dcos" {
  domain_name = "${var.aws_region}.${lookup(var.dns_domainnames, var.aws_region)}"
  domain_name_servers = ["AmazonProvidedDNS"]
}

resource "aws_vpc_dhcp_options_association" "dcos" {
  vpc_id = "${aws_vpc.dcos.id}"
  dhcp_options_id = "${aws_vpc_dhcp_options.dcos.id}"
}
