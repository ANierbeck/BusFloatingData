resource "aws_security_group" "master_lb" {
  name = "master_lb"
  description = "Mesos Master LB"

  vpc_id = "${aws_vpc.dcos.id}"
}

resource "aws_security_group_rule" "master_lb_ingress_slave_2181" {
  security_group_id = "${aws_security_group.master_lb.id}"

  type = "ingress"
  from_port = 2181
  to_port = 2181
  protocol = "tcp"
  source_security_group_id = "${aws_security_group.slave.id}"
}

resource "aws_security_group_rule" "master_lb_egress_all" {
  security_group_id = "${aws_security_group.master_lb.id}"

  type = "egress"
  from_port = 0
  to_port = 65535
  protocol = "-1"
  cidr_blocks = ["0.0.0.0/0"]
}
