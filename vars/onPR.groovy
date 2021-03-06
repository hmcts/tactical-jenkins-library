/*
 * onPR
 *
 * Runs the block of code if the current branch is not master
 *
 * onPR {
 *   ...
 * }
 */
def call(block) {
  if (env.BRANCH_NAME.startsWith('PR')) {
    return block.call()
  }
}

