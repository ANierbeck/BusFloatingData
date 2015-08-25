resource "aws_network_acl" "public" {
  vpc_id = "${aws_vpc.dcos.id}"
  subnet_ids = ["${aws_subnet.public.id}"]

  egress {
    protocol = "-1"
    rule_no = 100
    action = "allow"
    cidr_block =  "0.0.0.0/0"
    from_port = 0
    to_port = 0
  }

  ingress {
    protocol = "-1"
    rule_no = 100
    action = "allow"
    cidr_block =  "0.0.0.0/0"
    from_port = 0
    to_port = 0
  }

  tags {
    Application = "${var.stack_name}"
    Network = "Public"
  }
}
