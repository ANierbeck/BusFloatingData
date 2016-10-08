resource "aws_elb" "vpn" {
  name = "vpn-${var.elb_version}-load-balancer"
  subnets = ["${aws_subnet.public.id}"]

  instances = ["${aws_instance.vpn.id}"]

  security_groups = [
    "${aws_security_group.vpn.id}",
    "${aws_security_group.admin.id}"
  ]

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 2
    timeout = 5
    target = "tcp:443"
    interval = 30
  }

  listener {
    instance_port = 443
    instance_protocol = "tcp"
    lb_port = 443
    lb_protocol = "tcp"
  }
}
