resource "aws_route_table" "private" {
  vpc_id = "${aws_vpc.dcos.id}"

  route {
    cidr_block = "0.0.0.0/0"
    instance_id = "${aws_instance.nat.id}"
  }

  tags {
    Application = "${var.stack_name}"
    Network = "Private"
  }
}
