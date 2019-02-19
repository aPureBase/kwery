## 0.18.0 [Unreleased]
* Change to semver
* Move CI to gitlab

## Previous versions

`0.17` Compatible with Kotlin 1.1.3-2.
* Fix #14 - Incorrect parameter positions for collections
* Lazily set `Statement.poolable`

`0.16` Compatible with Kotlin 1.1.0.

`0.15` Compatible with Kotlin 1.0.4.
* Mapper: Support ThreadLocalSessions in Dao by creating implicit transactions (thanks @brianmadden)

`0.14` Compatible with Kotlin 1.0.4.

`0.13` Compatible with Kotlin 1.0.3.

`0.12` Compatible with Kotlin 1.0.2.
* Core: QueryBuilder
* Core: Fix collection binding when not first parameter
* Mapper: Add Dao.findByIdForUpdate

`0.11` Compatible with Kotlin 1.0.2.
* Core: Fix logging of statements with bound values containing `$`
* Core: Add experimental sqlite support
* Mapper: Support generated keys for MySQL in DAOs

`0.10` Compatible with Kotlin 1.0.2.

`0.9` Compatible with Kotlin 1.0.0.
* Mapper: add `Table.optionalCol` to construct optional types via paths

`0.8` Compatible with Kotlin 1.0.0-rc-1036.
* Mapper: support PreUpdate and PreInsert events (thanks @davemaple)
* Remove tomcat pool module as Postgres drivers now support prepared statement caching

`0.7` Compatible with Kotlin 1.0.0-beta-3595.
* Add MySQL dialect

`0.6` Compatible with Kotlin 1.0.0-beta-1038.

`0.5` Compatible with Kotlin M14.

`0.4` Compatible with Kotlin M13:
* Provide a consistent set of defaults and converters for mapping standard types
* Add defaults and converters for OffsetDateTime and ZonedDateTime

`0.3` Compatible with Kotlin M13:
* Improved docs
* Simplified transaction listeners
* Made transactions re-entrant
* Renamed ThreadLocalSession to ManagedThreadLocalSession and introduced a new ThreadLocalSession for
  use without interceptors and annotations.

`0.2` Compatible with Kotlin M12, adding transactional interceptors.

`0.1` Compatible with Kotlin M11.