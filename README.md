# DCOS Terraform Module

Using this [Terraform][] [module][], you can launch your own [DCOS][] cluster.

## Configurables

See [`variables.tf`](variables.tf) for a list of configurable parameters.

[Terraform]: https://www.terraform.io
[module]: https://www.terraform.io/docs/modules/index.html
[DCOS]: https://mesosphere.com/learn/

## Module Instructions

To include this module in your Terraform code-base, use the following snippet:

```hcl
module "dcos" {
  source = "github.com/jeanmertz/terraform-dcos"

  aws_access_key = "..."
  aws_secret_key = "..."
  aws_region     = "eu-central-1"
  ssh_public_key = "ssh-rsa ..."

  ...
}
```

Then run `terraform get` to retrieve this module.

## Stand-Alone Instructions

Any Terraform module can also be used on its own. To do so, follow these
instructions:

* clone the repository
* create a `terraform.tfvars` file with all the (required) variables
```vim
aws_access_key="*****"
aws_secret_key="*****"
aws_region="eu-west-1"
ssh_public_key="ssh-rsa ***** bernd.zuther@codecentric.de"
```
* *optionally* run `terraform plan -out terraform.plan`
* run `terraform apply [terraform.plan]`

## Dependency Graph
