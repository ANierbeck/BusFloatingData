resource "aws_elb" "internal_master" {
  name = "master-${var.elb_version}-load-balancer"
  internal = true
  subnets = ["${aws_subnet.master.id}"]

  security_groups = [
    "${aws_security_group.master_lb.id}",
    "${aws_security_group.admin.id}",
    "${aws_security_group.slave.id}",
    "${aws_security_group.public_slave.id}",
    "${aws_security_group.master.id}"
  ]

  health_check {
    healthy_threshold = 2
    unhealthy_threshold = 2
    timeout = 5
    target = "HTTP:5050/health"
    interval = 30
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
