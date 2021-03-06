package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserModelTest extends AbstractModelTest {

    @Test
    public void persistUser() {
        RealmModel realm = realmManager.createRealm("original");
        KeycloakSession session = realmManager.getSession();
        UserModel user = session.users().addUser(realm, "user");
        user.setFirstName("first-name");
        user.setLastName("last-name");
        user.setEmail("email");
        assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.addRequiredAction(RequiredAction.UPDATE_PASSWORD);

        RealmModel searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted = session.users().getUserByUsername("user", searchRealm);

        assertEquals(user, persisted);

        searchRealm = realmManager.getRealm(realm.getId());
        UserModel persisted2 =  session.users().getUserById(user.getId(), searchRealm);
        assertEquals(user, persisted2);

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(UserModel.LAST_NAME, "last-name");
        List<UserModel> search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");

        attributes.clear();
        attributes.put(UserModel.LAST_NAME, "last-name");
        attributes.put(UserModel.EMAIL, "email");
        search = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(search.size(), 1);
        Assert.assertEquals(search.get(0).getUsername(), "user");
    }
    
    @Test
    public void webOriginSetTest() {
        RealmModel realm = realmManager.createRealm("original");
        ClientModel client = realm.addClient("user");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client = realm.addClient("oauthclient2");

        Assert.assertTrue(client.getWebOrigins().isEmpty());

        client.addWebOrigin("origin-1");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.addWebOrigin("origin-2");
        Assert.assertEquals(2, client.getWebOrigins().size());

        client.removeWebOrigin("origin-2");
        Assert.assertEquals(1, client.getWebOrigins().size());

        client.removeWebOrigin("origin-1");
        Assert.assertTrue(client.getWebOrigins().isEmpty());

    }

    @Test
    public void testUserRequiredActions() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = session.users().addUser(realm, "user");

        Assert.assertTrue(user.getRequiredActions().isEmpty());

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        String id = realm.getId();
        commit();
        realm = realmManager.getRealm(id);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));

        user.addRequiredAction(RequiredAction.VERIFY_EMAIL.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(2, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.CONFIGURE_TOTP.name()));
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        user.removeRequiredAction(RequiredAction.CONFIGURE_TOTP.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertEquals(1, user.getRequiredActions().size());
        Assert.assertTrue(user.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        user.removeRequiredAction(RequiredAction.VERIFY_EMAIL.name());
        user = session.users().getUserByUsername("user", realm);

        Assert.assertTrue(user.getRequiredActions().isEmpty());
    }

    @Test
    public void testUserMultipleAttributes() throws Exception {
        RealmModel realm = realmManager.createRealm("original");
        UserModel user = session.users().addUser(realm, "user");

        user.setSingleAttribute("key1", "value1");
        List<String> attrVals = new ArrayList<>(Arrays.asList( "val21", "val22" ));
        user.setAttribute("key2", attrVals);

        commit();

        // Test read attributes
        realm = realmManager.getRealmByName("original");
        user = session.users().getUserByUsername("user", realm);

        attrVals = user.getAttribute("key1");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("value1", attrVals.get(0));
        Assert.assertEquals("value1", user.getFirstAttribute("key1"));

        attrVals = user.getAttribute("key2");
        Assert.assertEquals(2, attrVals.size());
        Assert.assertTrue(attrVals.contains("val21"));
        Assert.assertTrue(attrVals.contains("val22"));

        attrVals = user.getAttribute("key3");
        Assert.assertTrue(attrVals.isEmpty());
        Assert.assertNull(user.getFirstAttribute("key3"));

        Map<String, List<String>> allAttrVals = user.getAttributes();
        Assert.assertEquals(2, allAttrVals.size());
        Assert.assertEquals(allAttrVals.get("key1"), user.getAttribute("key1"));
        Assert.assertEquals(allAttrVals.get("key2"), user.getAttribute("key2"));

        // Test searching
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("key2", "val22");
        List<UserModel> users = session.users().searchForUserByAttributes(attributes, realm);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(users.get(0), user);

        // Test remove and rewrite attribute
        user.removeAttribute("key1");
        user.setSingleAttribute("key2", "val23");

        commit();

        realm = realmManager.getRealmByName("original");
        user = session.users().getUserByUsername("user", realm);
        Assert.assertNull(user.getFirstAttribute("key1"));
        attrVals = user.getAttribute("key2");
        Assert.assertEquals(1, attrVals.size());
        Assert.assertEquals("val23", attrVals.get(0));
    }

    public static void assertEquals(UserModel expected, UserModel actual) {
        Assert.assertEquals(expected.getUsername(), actual.getUsername());
        Assert.assertEquals(expected.getCreatedTimestamp(), actual.getCreatedTimestamp());
        Assert.assertEquals(expected.getFirstName(), actual.getFirstName());
        Assert.assertEquals(expected.getLastName(), actual.getLastName());

        String[] expectedRequiredActions = expected.getRequiredActions().toArray(new String[expected.getRequiredActions().size()]);
        Arrays.sort(expectedRequiredActions);
        String[] actualRequiredActions = actual.getRequiredActions().toArray(new String[actual.getRequiredActions().size()]);
        Arrays.sort(actualRequiredActions);

        Assert.assertArrayEquals(expectedRequiredActions, actualRequiredActions);
    }

}

