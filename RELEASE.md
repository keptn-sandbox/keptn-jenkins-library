# Releasing

A new version of the Keptn Jenkins library is released as needed when
there are fixes and features ready.

## Versioning

The versioning of the Keptn Jenkins library follows [semver](https://semver.org/).
This means, every version number consists of three parts (x.y.z) that have a
different meaning and describe the type of change this version introduces.

- **Patch:** (x.y.**z**): A patch version only includes fixes and small patches.
- **Minor:** (x.**y**.z): A minor version includes new features that do **NOT**
  introduce a breaking change.
- **Major:** (**x**.y.z): A major version is the only version that can contain
  breaking changes.

Version numbers will be calculated based on commit messages. For that purpose
please refer to [conventional
commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) to correctly
categorize changes for the next version number.

## How to release

### Typical case: create a new version based on latest release

New versions are created using `Pre-Release` and `Release` workflows available
in the [`Actions`
tab](https://github.com/keptn-sandbox/keptn-jenkins-library/actions) on GitHub.
The worflows will take care of calculating the next version number and create a
GitHub release depending on the type of changes introduced.

Workflows must be run on the appropriate branch (`master` should be the normal
case, for special cases see the next section).

A new GitHub Release will be created as a draft and a Pull Request will be
opened describing the changes in [CHANGELOG.md](CHANGELOG.md).

### Special cases: bugfixing and hotfixes for older versions

**Important:** Bugs must be fixed on `master` branch first and then backported to the
appropriate <major>.<minor> versions.

If the target major and minor versions onto which we have to backport the fix
are the same as the latest version released from master the usual procedure
described in the previous paragraph is sufficient and a new release can be
created increasing the patch number of the latest release.

If the fixes must be ported to older library versions, a maintenance branch
starting from the last commit on master tagged with the same
major and minor version should be used (creating it if necessary) using the
format `<major version>.<minor version>.x`.

Once the backport of the fix is present on the maintenance branch a new release
for the same major/minor version can be created running `Pre-Release` or
`Release` workflow on such branch.

For *critical* fixes a hotfix for a specific patch version can be created using
a `hotfix branch` that starts at the **exact major, minor and patch version**
using the format `<major version>.<minor version>.<patch version>-hotfix`.
Normal `Pre-Release` and `Release` workflow will not calculate the version
correctly, so the release will have to be created manually tagging the version
as `x.y.z-**n**` where n is the progressive number of the hotfixes applied
(hopefully only 1 is needed).

Contrary to the bugfix process, hotfix releases should be used only in
exceptional cases where it's impossible for the user to move to a newer patch
version. The need for hotfixes will be evaluated on a case-by-case basis
depending on the impact of the issue.
