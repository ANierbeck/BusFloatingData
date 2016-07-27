resource "aws_route_table_association" "master" {
  subnet_id = "${aws_subnet.master.id}"
  route_table_id = "${aws_route_table.master.id}"
}
