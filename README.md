# Earth Observation branch of Thredds Data Server 4.2

This is a branch of Thredds Data Server 4.2 with enhancements to support Earth Observation (satellite imagery).


## Features

* ncWMS scanline support for NetCDF4 / HDF5, necessary for large data files.
* ncWMS support for large source grids (with more than 2**31-1 points).
    * For example, WMS for a 647 GB NetCDF 4 file with a single `ubyte` variable on a 461276x1505407 grid.
* Support for false colour WMS layers created from NetCDF variables.


## False colour WMS

To enable a false colour layer, edit your `wmsConfig.xml` to specify a new layer name (`id`) and the three NetCDF source variables that form its components by inserting a `variable` element in `wmsConfig/overrides/datasetPath/variables`. This example creates a new false colour layer `FalseColour741` with red, green, and blue components from variables `Band7`, `Band4`, and `Band1` respectively:

    <variable id="FalseColour741">
        <title>False Colour</title>
        <abstract>False Colour from bands 7, 4, and 1</abstract>
        <range>1 10000</range>
        <gamma>0.5</gamma>
        <red>
            <source>Band7</source>
            <range>10 7500</range>
            <gamma>0.7</gamma>
        </red>
        <green>
            <source>Band4</source>
        </green>
        <blue>
            <source>Band1</source>
        </blue>
    </variable>

* `title` and `abstract` are optional. If `title` is omitted, a default will be created from the component source layer names. If `abstract` is omitted, it will be set to the same value as `title`.
* `range` and `gamma` can be given either in the  `variable` element or inside the element for a colour component, with the latter taking precedence.
* `source` (required) is the name of the NetCDF variable from which data is taken to determine the value of a particular colour channel.
* `range` (optional) is the range of valid data values in the source variable; values outside this range are treated as missing. If omitted, `range` defaults to `0 255`.
* `gamma` (optional) is the exponent applied to the fraction of the range before conversion to a colour: `((x - xmin) / (xmax - xmin)) ** gamma`. If omitted, `gamma` defaults to `1.0`.

Note that, if you are updating the `wmsConfig.xml` of an upstream (Unidata-built) Thredds Data Server 4.2 deployment, the `DOCTYPE` of `wmsConfig.xml` has changed; if you want to use false colour layers, the `DOCTYPE` of `wmsConfig.xml` must be updated from this branch. The format of `wmsConfig.xml` is backwards-compatible.


## Build

To build Thredds you need an Oracle JDK 1.6 and Apache Ant 1.8.

Clone these git repositories into some `${BASEDIR}`:

    cd ${BASEDIR}
    git clone -b earthobs https://github.com/bencaradocdavies/ncwms-tds42 ncwms
    git clone -b earthobs https://github.com/bencaradocdavies/tds42-lib thredds/lib
    git clone -b earthobs https://github.com/bencaradocdavies/tds42 thredds/tds

Build ncWMS:

    cd ${BASEDIR}/ncwms
    ant -f buildTds.xml

Build Thredds:

    cd ${BASEDIR}/thredds/tds
    ant

Your new Thredds deployment bundle is: `${BASEDIR}/thredds/tds/target/thredds.war`

Note that the ncWMS build pushes `ncwms.jar` into `${BASEDIR}/thredds/lib`. Note also that the Thredds build expects `lib` to be a peer directory of the Thredds build directory. If you change your relative build paths, you will have to modify the build files or set properties on the ant command line.


## Eclipse

Eclipse project information is included in this branch. All three projects should be imported into your Eclipse workspace to satisfy dependencies.


## Branch maintenance

This section is intended for developers who are maintaining this branch and wish to merge new changes from the upstream subversion repositories. This task is made a great deal easier by the presence of the `master` branch, which tracks the upstream subversion repositories, but requires a different method of cloning to enable use of git-svn; simply cloning the git repositories does not work because they are missing git-svn configuration information. You are also strongly advised to set "`git config core.autocrlf false`" as these projects contain an interesting mix of line endings that are most unpalatable to git-svn without this setting.

The `master` branch of this repository was created by cloning the upstream subversion repositories:

    git svn clone -r 767:HEAD https://ncwms.svn.sourceforge.net/svnroot/ncwms/branches/tds4.2-20101102 ncwms
    git svn clone -r 14585:HEAD http://svn.unidata.ucar.edu/repos/thredds/branch/RB-TDS-4.2/lib thredds/lib
    git svn clone -r 11338:HEAD http://svn.unidata.ucar.edu/repos/thredds/branch/RB-TDS-4.2/tds thredds/tds

To use "`git svn rebase`" on the `master` branch, clone the subversion repositories, then in each of the three local working directories add the git repositories as an external, fetch to discover new branches, checkout `earthobs` to set it up as a tracking branch, then check out `master` again. This procedure is as follows:

    cd ${BASEDIR}
    git svn clone -r 767:HEAD https://ncwms.svn.sourceforge.net/svnroot/ncwms/branches/tds4.2-20101102 ncwms
    cd ${BASEDIR}/ncwms
    git config core.autocrlf false
    git remote add bencaradocdavies https://github.com/bencaradocdavies/ncwms-tds42
    git fetch bencaradocdavies
    git checkout -b earthobs bencaradocdavies/earthobs
    git checkout master

    cd ${BASEDIR}
    git svn clone -r 14585:HEAD http://svn.unidata.ucar.edu/repos/thredds/branch/RB-TDS-4.2/lib thredds/lib
    cd ${BASEDIR}/thredds/lib
    git config core.autocrlf false
    git remote add bencaradocdavies https://github.com/bencaradocdavies/tds42-lib
    git fetch bencaradocdavies
    git checkout -b earthobs bencaradocdavies/earthobs
    git checkout master

    cd ${BASEDIR}
    git svn clone -r 11338:HEAD http://svn.unidata.ucar.edu/repos/thredds/branch/RB-TDS-4.2/tds thredds/tds
    cd ${BASEDIR}/thredds/tds
    git config core.autocrlf false
    git remote add bencaradocdavies https://github.com/bencaradocdavies/tds42
    git fetch bencaradocdavies
    git checkout -b earthobs bencaradocdavies/earthobs
    git checkout master

This procedure should give you a git working directory that can be used for branch maintenance. There might be more elegant ways to retrofit git-svn cruft on cloned git repositories; this is a matter for further investigation.


