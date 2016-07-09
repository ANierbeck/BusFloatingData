resource "aws_subnet" "public" {
  vpc_id = "${aws_vpc.dcos.id}"
  cidr_block = "${var.public_subnet_range}"

  tags {
    Application = "${var.stack_name}"
    Network = "Public"
  }
}
