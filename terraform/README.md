# DCOS Multi Master Terraform Module

Using this [Terraform][] [module][], you can launch your own [DCOS][] cluster.

The Mesosphere Datacenter Operating System (DCOS) spans all of the machines in your datacenter or cloud and treats 
them as a single, shared set of resources. DCOS provides a highly elastic and highly scalable way to build, deploy and 
manage modern applications using containers, microservices and big data systems.git 

![DCOS](/doc/images/dcos.png)

## Configurables

See [`variables.tf`](variables.tf) for a list of configurable parameters.

[Terraform]: https://www.terraform.io
[module]: https://www.terraform.io/docs/modules/index.html
[DCOS]: https://mesosphere.com/learn/

## Module Instructions

To include this module in your Terraform code-base, use the following snippet:

```hcl
module "dcos" {
  source = "github.com/zutherb/terraform-dcos"

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

openvpn_admin_user="openvpn"
openvpn_admin_pw="******"
```
* *optionally* run `terraform plan -out terraform.plan`
* run `terraform apply [terraform.plan]`

```bash
```

## Architecture

DCOS is based on [Mesos](http://mesos.apache.org/) and includes a distributed systems kernel. It also includes a set 
of core system services, such as a native [Marathon](https://mesosphere.github.io/marathon/) instance to manage processes 
and installable services, and [Mesos-DNS](https://github.com/mesosphere/mesos-dns) for service discovery.

### Components

DCOS is comprised of Mesos master and agent nodes, a native DCOS Marathon instance, Mesos-DNS for service 
discovery, Admin Router for central authentication and proxy to DCOS services, and Zookeeper to coordinate and manage 
the installed DCOS services.

![DCOS](https://docs.mesosphere.com/wp-content/uploads/2015/12/Enterprise-Architecture-Diagram.png)

[Read more](https://docs.mesosphere.com/administration/dcosarchitecture/components/)

### Network Security

DCOS provides the admin, private, and public security zones. The admin zone is accessible via HTTP/HTTPS and SSH 
connections, and provides access to your master nodes. The private zone is a non-routable network that is only 
accessible from the admin zone or through the edgerouter from the public zone. The optional public zone is where 
publicly accessible applications are run. 

![DCOS](https://docs.mesosphere.com/wp-content/uploads/2015/12/security-zones-ce.jpg)

[Read more](https://docs.mesosphere.com/administration/dcosarchitecture/security/)

## Limitations

- The DCOS Community Edition does not provide authentication. Authentication is available in the DCOS Enterprise Edition.
- The DCOS CLI and web interface do not currently use an encrypted channel for communication. However, you can upload 
  your own SSL certificate to the masters and change your CLI and web interface configuration to use HTTPS instead of HTTP.
- You must secure your cluster by using security rules. It is strongly recommended that you only allow internal traffic.
- If there is sensitive data in your cluster, follow standard cloud policies for accessing that data. Either set up a 
  point to point VPN between your secure networks or run a VPN server inside your DCOS cluster.

## OpenVPN

OpenVPN Access Server is a full featured secure network tunneling VPN software solution that integrates OpenVPN server 
capabilities, enterprise management capabilities, simplified OpenVPN Connect UI, and OpenVPN Client software packages 
that accommodate Windows, MAC, Linux, Android, and iOS environments. OpenVPN Access Server supports a wide range of 
configurations, including secure and granular remote access to internal network and/ or private cloud network resources 
and applications with fine-grained access control. 