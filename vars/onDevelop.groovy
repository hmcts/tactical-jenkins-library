/*
 * onDevelop
 *
 * Runs the block of code if the current branch is develop
 *
 * onDevelop {
 *   ...
 * }
 */
def call(block) {
  if (env.BRANCH_NAME == 'develop') {
    return block.call()
  }
}
