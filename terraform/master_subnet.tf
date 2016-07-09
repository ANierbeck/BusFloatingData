resource "aws_subnet" "master" {
  vpc_id = "${aws_vpc.dcos.id}"
  cidr_block = "${var.master_subnet_range}"

  tags {
    Application = "${var.stack_name}"
    Network = "Master"
  }
}
