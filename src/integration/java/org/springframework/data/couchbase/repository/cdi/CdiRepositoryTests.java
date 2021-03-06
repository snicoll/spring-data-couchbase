/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.couchbase.repository.cdi;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;
import org.apache.webbeans.cditest.CdiTestContainer;
import org.apache.webbeans.cditest.CdiTestContainerLoader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class CdiRepositoryTests {

	private static CdiTestContainer cdiContainer;
	private CdiPersonRepository repository;
	private Bucket couchbaseClient;

	@BeforeClass
	public static void init() throws Exception {
		cdiContainer = CdiTestContainerLoader.getCdiContainer();
		cdiContainer.startApplicationScope();
		cdiContainer.bootContainer();
	}

	@AfterClass
	public static void shutdown() throws Exception {
		cdiContainer.stopContexts();
		cdiContainer.shutdownContainer();
	}

	@Before
	public void setUp() {
		CdiRepositoryClient repositoryClient = cdiContainer.getInstance(CdiRepositoryClient.class);
		repository = repositoryClient.getCdiPersonRepository();

		couchbaseClient = repositoryClient.getCouchbaseClient();
		createAndWaitForDesignDocs(couchbaseClient);

	}

	private void createAndWaitForDesignDocs(Bucket client) {
		String mapFunction = "function (doc, meta) { if(doc._class == \"" + Person.class.getName()
				+ "\") { emit(null, null); } }";
		View view = DefaultView.create("all", mapFunction, "_count");
		List<View> views = Collections.singletonList(view);
		DesignDocument designDoc = DesignDocument.create("person", views);
		client.bucketManager().upsertDesignDocument(designDoc);
	}

	/**
	 * @see DATACOUCH-109
	 */
	@Test
	public void testCdiRepository() {
		assertNotNull(repository);
		repository.deleteAll();

		Person bean = new Person("key", "username");

		repository.save(bean);

		assertTrue(repository.exists(bean.getId()));

		Person retrieved = repository.findOne(bean.getId());
		assertNotNull(retrieved);
		assertEquals(bean.getName(), retrieved.getName());
		assertEquals(bean.getId(), retrieved.getId());

	}

	/**
	 * @see DATACOUCH-109
	 */
	@Test
	public void testCustomRepository() {

		assertEquals(2, repository.returnTwo());
	}

}
