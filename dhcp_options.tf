resource "aws_vpc_dhcp_options" "dcos" {
  domain_name = "${var.aws_region}.compute.internal"
  domain_name_servers = ["AmazonProvidedDNS"]
}

resource "aws_vpc_dhcp_options_association" "dcos" {
  vpc_id = "${aws_vpc.dcos.id}"
  dhcp_options_id = "${aws_vpc_dhcp_options.dcos.id}"
}
