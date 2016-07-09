resource "aws_security_group" "vpn" {
  name = "vpn"
  description = "Mesos VPN"

  vpc_id = "${aws_vpc.dcos.id}"
}

resource "aws_security_group_rule" "vpn_egress_all" {
  security_group_id = "${aws_security_group.vpn.id}"

  type = "egress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  cidr_blocks = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "vpn_ingress_443" {
  security_group_id = "${aws_security_group.vpn.id}"

  type = "ingress"
  from_port = 443
  to_port = 443
  protocol = "tcp"

  self = true
}

resource "aws_security_group_rule" "vpn_ingress_943" {
  security_group_id = "${aws_security_group.vpn.id}"

  type = "ingress"
  from_port = 943
  to_port = 943
  protocol = "tcp"

  self = true
}

resource "aws_security_group_rule" "vpn_ingress_1194" {
  security_group_id = "${aws_security_group.vpn.id}"

  type = "ingress"
  from_port = 1194
  to_port = 1194
  protocol = "udp"

  self = true
}

