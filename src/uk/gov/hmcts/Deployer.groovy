package uk.gov.hmcts

import com.cloudbees.groovy.cps.NonCPS

/**
 * @deprecated use uk.gov.hmcts.Ansible instead
 */
@Deprecated
class Deployer implements Serializable {
  def products
  def _product = []

  @NonCPS
  def getProduct() {
    if ('product' in _product) return _product
    _product = products.find { p -> p.product == productName }
    if (_product == null) {
      error "$productName is not one of reforms products"
    }
    return _product
  }

  def profile(environment) {
    def product = getProduct()
    if ('profile' in product) {
      return profile.profile
    } else {
      if (environment in ['dev', 'demo', 'test', 'prod']) {
        return environment
      } else {
        error "$environment is not one of reforms deployment environments"
      }
    }
  }

  def env(environment) {
    def product = getProduct()
    if ('profile' in product) {
      return product.profile
    } else {
      return environment
    }
  }

  def repo() {
    return getProduct().repo
  }

  def channel() {
    return getProduct().channel
  }

  def tags(env) {
    def tags = findTags(env)
    if (tags) {
      return tags.azure_tags
    } else {
      return getProduct().tags
    }
  }

  @NonCPS()
  def findTags(env) {
    return getProduct().tags.find { l -> l.environment == env }
  }

  @NonCPS()
  def findLimit(env) {
    return getProduct().limit.find { l -> l.environment == env }
  }

  def limit(environment, play) {
    if ('limit' in getProduct()) {
      def limit = findLimit(environment)
      if (limit) {
        return limit.ansible_pattern
      } else {
        steps.error "Can't run ${repo()}/${play} against ${environment}"
      }
    } else {
      return 'all'
    }
  }
  def steps
  def productName

  Deployer(steps, productName) {
    this.steps = steps
    this.productName = productName

    this.products =  new groovy.json.JsonSlurperClassic()
      .parseText(steps.libraryResource('uk/gov/hmcts/products.json'))
  }

  def deploy(versions, environment, play, ansible_branch='master') {
    steps.git url: "git@git.reform.hmcts.net:${(repo())}", branch: ansible_branch
    steps.sh "ansible-galaxy install -r requirements.yml --force --roles-path=roles/"

    def azureInventoryFile = steps.libraryResource 'uk/gov/hmcts/azure_rm.py'
    steps.writeFile file: './inventory/azure_rm.py', text: azureInventoryFile

    steps.sh """
        chmod +x ./inventory/azure_rm.py

        export ANSIBLE_HOST_KEY_CHECKING='False'
        export AZURE_PROFILE="${profile(environment)}"
        export AZURE_GROUP_BY_TAG='yes'
        export AZURE_TAGS="${tags(environment)}"
        export ANSIBLE_ROLES_PATH='roles/'
        export VAULT_ADDR='https://vault.reform.hmcts.net:6200'

        ansible-playbook "${play}" \
        -i ./inventory \
        --limit "${limit(environment, play)}" \
        --extra-vars "deploy_target=${env(environment)}" \
        --extra-vars "{'versions': ${versions} }"
        """
  }
}

