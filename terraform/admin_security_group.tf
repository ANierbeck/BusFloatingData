resource "aws_security_group" "admin" {
  name = "admin"
  description = "Enable admin access to servers"

  vpc_id = "${aws_vpc.dcos.id}"
}

resource "aws_security_group_rule" "admin_ingress_all" {
  security_group_id = "${aws_security_group.admin.id}"

  type = "ingress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  cidr_blocks = ["${var.admin_location}"]
}

resource "aws_security_group_rule" "admin_egress_all" {
  security_group_id = "${aws_security_group.admin.id}"

  type = "egress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  cidr_blocks = ["0.0.0.0/0"]
}
