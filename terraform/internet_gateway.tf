resource "aws_internet_gateway" "dcos" {
  vpc_id = "${aws_vpc.dcos.id}"

  tags {
    Application = "${var.stack_name}"
    Network = "Public"
  }
}
