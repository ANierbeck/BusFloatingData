resource "aws_security_group" "master" {
  name = "master-${var.stack_name}"
  description = "Mesos Masters"

  vpc_id = "${aws_vpc.dcos.id}"
}

resource "aws_security_group_rule" "master_egress_all" {
  security_group_id = "${aws_security_group.master.id}"

  type = "egress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  cidr_blocks = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "master_ingress_master" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  self = true
}

resource "aws_security_group_rule" "master_ingress_public_slave" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  source_security_group_id = "${aws_security_group.public_slave.id}"
}

resource "aws_security_group_rule" "master_ingress_slave" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  source_security_group_id = "${aws_security_group.slave.id}"
}

resource "aws_security_group_rule" "master_ingress_master_lb_5050" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 5050
  to_port = 5050
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.master_lb.id}"
}

resource "aws_security_group_rule" "master_ingress_master_lb_80" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 80
  to_port = 80
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.master_lb.id}"
}

resource "aws_security_group_rule" "master_ingress_master_lb_8080" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 8080
  to_port = 8080
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.master_lb.id}"
}

resource "aws_security_group_rule" "master_ingress_master_lb_8081" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 8081
  to_port = 8081
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.master_lb.id}"
}

resource "aws_security_group_rule" "master_ingress_master_lb_2181" {
  security_group_id = "${aws_security_group.master.id}"

  type = "ingress"
  from_port = 2181
  to_port = 2181
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.master_lb.id}"
}
