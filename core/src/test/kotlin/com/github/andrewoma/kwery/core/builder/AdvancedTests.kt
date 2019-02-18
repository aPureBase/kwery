package com.github.andrewoma.kwery.core.builder

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should equal`
import org.junit.Test

class AdvancedTests {

    @Test fun `Multiple root where groups`() {
        query {
            select("SELECT * FROM PERSON p")
            whereGroup {
                where("p.PERS_ID = '25'")
            }
            whereGroup {
                where("p.SITE_ID = '100'")
            }
            whereGroup {
                where("p.MERGE = '500'")
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            where
            (p.PERS_ID = '25' and p.SITE_ID = '100') and p.MERGE = '500'
        """.trimQuery()
    }

    @Test fun `Single root where group`() {
        query {
            select("SELECT * FROM PERSON p")
            whereGroup {
                where("p.PERS_ID = '25'")
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            where p.PERS_ID = '25'
        """.trimQuery()
    }

    @Test fun `Two root where groups`() {
        query {
            select("SELECT * FROM PERSON p")
            whereGroup {
                where("p.PERS_ID = '25'")
            }
            whereGroup {
                where("p.SITE_ID = '500'")
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            where p.PERS_ID = '25' and p.SITE_ID = '500'
        """.trimQuery()
    }

    @Test fun `Nested and multi where groups`() {
        query {
            select("SELECT * FROM PERSON p")
            whereGroup {
                where("p.PERS_ID = '25'")
            }
            whereGroup {
                whereGroup {
                    where("p.SITE_ID = '500'")
                    where("p.SITE_ID = '500'")
                    where("p.SITE_ID = '500'")
                }
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            where p.PERS_ID = '25' and
            (p.SITE_ID = '500' and p.SITE_ID = '500' and p.SITE_ID = '500')
        """.trimQuery()
    }

    @Test fun `Multiple join groups`() {
        query {
            select("SELECT * FROM PERSON p")
            joinGroup {
                innerJoin("SITE_PERS", "sp") { "$it.PERS_ID = p.PERS_ID" }
            }
            joinGroup {
                innerJoin("SITE_PERS2", "sp2") { "$it.PERS_ID = p.PERS_ID" }
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            inner join SITE_PERS sp on sp.PERS_ID = p.PERS_ID
            inner join SITE_PERS2 sp2 on sp2.PERS_ID = p.PERS_ID
        """.trimQuery()
    }

    @Test fun `Multiple join and where groups`() {
        query {
            select("SELECT * FROM PERSON p")
            whereGroup {
                where("p.PERS_ID = '25'")
            }
            whereGroup {
                where("p.SITE_ID = '100'")
            }
            joinGroup {
                innerJoin("SITE_PERS", "sp") { "$it.PERS_ID = p.PERS_ID" }
                innerJoin("SITE_PERS", "sp2") { "$it.PERS_ID = p.PERS_ID" }
            }
            joinGroup {
                leftJoin("SOMETHING") { "$it.APB_ID = sp.Q_ID" }
            }
        }.sql.trimQuery() `should be equal to` """
            SELECT * FROM PERSON p
            inner join SITE_PERS sp on sp.PERS_ID = p.PERS_ID
            inner join SITE_PERS sp2 on sp2.PERS_ID = p.PERS_ID
            left join SOMETHING s on s.APB_ID = sp.Q_ID
            where p.PERS_ID = '25' and p.SITE_ID = '100'
        """.trimQuery()
    }

    @Test fun `Creating an where extension`() {
        fun QueryBuilder.FilterBuilder.where() {
            where("a = :b")
            parameter(":b", "abc")
        }
        query {
            whereGroup {
                where()
            }
        }.parameters `should equal` mapOf(":b" to "abc")
    }


    @Test fun `Simple union sample`() {
        (query {
            select("SELECT * FROM SITE")
        } union query {
            select("SELECT * FROM PERSON")
        }).sql `should be equal to` "SELECT * FROM SITE UNION SELECT * FROM PERSON"
    }

    @Test fun `Unions with parameters`() {
        val result = query {
            select("SELECT p.ID AS ID, p.NAME FROM dbo.PERSON p")
            whereGroup {
                where("p.ID = :id")
                parameter("id", 25)
            }
        } unionAll query {
            select("SELECT dp.PERS_ID as ID, dp.PERS_NAME as NAME FROM dbi.PERSON dp")
            joinGroup {
                leftJoin("SITE_PERS", "sp") { "$it.SITE_ID = dp.SITE_ID" }
            }
            whereGroup {
                where("p.ID = :dpid")
                parameter("dpid", 25)
            }
        }

        result.sql `should be equal to` """
            SELECT p.ID AS ID, p.NAME FROM dbo.PERSON p
            where p.ID = :id UNION ALL SELECT dp.PERS_ID as ID, dp.PERS_NAME as NAME FROM dbi.PERSON dp
            left join SITE_PERS sp on sp.SITE_ID = dp.SITE_ID
            where p.ID = :dpid
        """.trimIndent()

        result.parameters["id"] `should equal` 25
        result.parameters["dpid"] `should equal` 25
        result.parameters.size `should be equal to` 2
    }
}
