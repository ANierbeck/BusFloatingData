resource "aws_elb" "dcos" {
  name = "dcos-load-balancer"
  subnets = ["${aws_subnet.public.id}"]

  security_groups = [
    "${aws_security_group.master_lb.id}",
    "${aws_security_group.admin.id}"
  ]

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 2
    timeout = 5
    target = "HTTP:5050/health"
    interval = 30
  }

  listener {
    instance_port = 22
    instance_protocol = "tcp"
    lb_port = 2222
    lb_protocol = "tcp"
  }

  listener {
    instance_port = 5050
    instance_protocol = "http"
    lb_port = 5050
    lb_protocol = "http"
  }

  listener {
    instance_port = 2181
    instance_protocol = "tcp"
    lb_port = 2181
    lb_protocol = "tcp"
  }

  listener {
    instance_port = 8181
    instance_protocol = "http"
    lb_port = 8181
    lb_protocol = "http"
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

  listener {
    instance_port = 8080
    instance_protocol = "http"
    lb_port = 8080
    lb_protocol = "http"
  }
}
