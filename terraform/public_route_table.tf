resource "aws_route_table" "public" {
  vpc_id = "${aws_vpc.dcos.id}"

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = "${aws_internet_gateway.dcos.id}"
  }

  tags {
    Application = "${var.stack_name}"
    Network = "Public"
  }
}
