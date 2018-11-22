package uk.gov.hmcts

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurperClassic

class Ansible implements Serializable {
  def products
  def _product = []

  def steps
  def productName

  def verbosestring

  Ansible(steps, productName) {
    this.steps = steps
    this.productName = productName

    this.products =  new JsonSlurperClassic()
      .parseText(steps.libraryResource('uk/gov/hmcts/products.json'))
  }

  def runInstallPlaybook(versions, environment, ansible_branch='master', azure_tags='', ansible_tags='all', verbose=false) {
    return run(versions, environment, 'install.yml', ansible_branch, azure_tags, ansible_tags, verbose)
  }

  def runDeployPlaybook(versions, environment, ansible_branch='master', azure_tags='', ansible_tags='all', verbose=false) {
    return run(versions, environment, 'deploy.yml', ansible_branch, azure_tags, ansible_tags, verbose)
  }

  def run(versions, environment, playbookName, ansible_branch='master', azure_tags='', ansible_tags='all', verbose=false) {
    // Generic checkout used to allow checking out of commit hashes
    // http://stackoverflow.com/a/43613408/4951015

    if(repo().toString().startsWith('hmcts/')){
      steps.checkout([$class: 'GitSCM',
                      branches: [[name: ansible_branch ]],
                      userRemoteConfigs: [[
                                            credentialsId: 'jenkins-public-github-api-token',
                                            url: "https://github.com/${(repo())}"
                                          ]]])
    } else {
      steps.checkout([$class: 'GitSCM', branches: [[name: ansible_branch ]],
                      userRemoteConfigs: [[url: "git@git.reform.hmcts.net:${(repo())}"]]])
    }

    steps.sh """
      ansible-galaxy install -r requirements.yml --force --roles-path=roles/
    """

    def azureInventoryFile = steps.libraryResource 'uk/gov/hmcts/azure_rm.py'
    steps.writeFile file: './inventory/azure_rm.py', text: azureInventoryFile
    steps.wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {

      def verbosestring = ""
      if (verbose) {
        verbosestring = " -vvv "
      }
      steps.sh """
          source venv/bin/activate
          chmod +x ./inventory/azure_rm.py

          export ANSIBLE_HOST_KEY_CHECKING='False'
          export AZURE_PROFILE="${profile(environment)}"
          export AZURE_GROUP_BY_TAG='yes'
          if [ "$azure_tags" == "" ]; then
            export AZURE_TAGS="${tags(environment)}"
          else
            export AZURE_TAGS="$azure_tags"
          fi
          export ANSIBLE_ROLES_PATH='roles/'
          export VAULT_ADDR='https://vault.reform.hmcts.net:6200'
          export ANSIBLE_FORCE_COLOR=true

          ansible-playbook ${verbosestring} "${playbookName}" \
          -i ./inventory \
          --limit "${limit(environment, playbookName)}" \
          --extra-vars "deploy_target=${env(environment)}" \
          --extra-vars "{'versions': ${versions} }" \
          --extra-vars "ansible_python_interpreter=venv/bin/python2" \
          --tags "${ansible_tags}"
          """
    }

    return steps.gitCommit()
  }

  private profile(environment) {
    def product = getProduct()
    if ('profile' in product) {
      return product.profile
    } else {
      if (environment in ['dev', 'demo', 'test', 'stage', 'preprod', 'prod', 'mgmt']) {
        return environment
      } else {
        error "$environment is not one of reforms deployment environments"
      }
    }
  }

  @NonCPS
  private getProduct() {
    if ('product' in _product) return _product
    _product = products.find { p -> p.product == productName }
    if (_product == null) {
      error "$productName is not one of reforms products"
    }
    return _product
  }

  private env(environment) {
    def product = getProduct()
    if ('profile' in product) {
      return product.profile
    } else {
      return environment
    }
  }

  private repo() {
    return getProduct().repo
  }

  private tags(env) {
    def tags = findTags(env)
    if (tags) {
      return tags.azure_tags
    } else {
      return getProduct().tags
    }
  }

  @NonCPS()
  private findTags(env) {
    return getProduct().tags.find { l -> l.environment == env }
  }

  @NonCPS()
  private findLimit(env) {
    return getProduct().limit.find { l -> l.environment == env }
  }

  private limit(environment, play) {
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
}
