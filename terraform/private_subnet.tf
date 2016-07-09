resource "aws_subnet" "private" {
  vpc_id = "${aws_vpc.dcos.id}"
  cidr_block = "${var.private_subnet_range}"

  tags {
    Application = "${var.stack_name}"
    Network = "Private"
  }
}
