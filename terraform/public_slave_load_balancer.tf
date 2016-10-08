resource "aws_elb" "public_slaves" {
  name = "public-slave-${var.elb_version}-load-balancer"

  subnets = ["${aws_subnet.public.id}"]

  security_groups = ["${aws_security_group.public_slave.id}"]

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 2
    timeout = 5
    target = "HTTP:80/"
    interval = 30
  }

  listener {
    instance_port = 80
    instance_protocol = "http"
    lb_port = 80
    lb_protocol = "http"
  }

  listener {
    instance_port = 443
    instance_protocol = "tcp"
    lb_port = 443
    lb_protocol = "tcp"
  }
}
